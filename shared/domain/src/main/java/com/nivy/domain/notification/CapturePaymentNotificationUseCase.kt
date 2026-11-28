package com.nivy.domain.notification

import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.data.repository.TransactionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/** How long the notifications read are kept: what the log shows. */
val NotificationsKeptFor: Duration = Duration.ofDays(30)

/** A notification Android posted: its app and the app's name, Android's key for it, when, its title and text. */
data class PostedNotification(
    val packageName: String,
    val appName: String?,
    val key: String,
    val postTime: Instant,
    val title: String?,
    val text: String?,
)

/** Where a notification's money comes from; [guessed] when its card's account isn't known yet. */
data class PaysFrom(val account: AccountId, val creditCard: CreditCardId?, val guessed: Boolean)

/**
 * Its card's account, when the card is known, else its app's; null when the app isn't picked. Only
 * Google Wallet's cards are a guess: they're any bank's, while a bank's own are its account's.
 */
fun paysFrom(sources: List<PaymentSourceEntity>, packageName: String, card: String?): PaysFrom? {
    val app = sources.find { it.kind == PaymentSourceEntity.App && it.id == packageName }
    val known = card?.let { digits -> sources.find { it.kind == PaymentSourceEntity.Card && it.id == digits } }
    return (known ?: app)?.takeIf { app != null }?.let { from ->
        PaysFrom(
            account = AccountId(from.accountId),
            creditCard = from.creditCardId?.let(::CreditCardId),
            guessed = card != null && known == null && packageName == PaymentNotificationReader.GoogleWallet,
        )
    }
}

/**
 * Reads the notifications of the apps picked for payment notifications, and keeps what became of
 * each: added as an expense, or would be while adding is off, the same payment as another app's,
 * not added, or ignored. One at a time, so two apps' notifications of one payment can't both be
 * added. Notifications are kept a month.
 */
@Singleton
class CapturePaymentNotificationUseCase @Inject constructor(
    private val repository: PaymentNotificationRepository,
    private val transactions: NotificationTransactions,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val appPreferences: AppPreferences,
    private val dataObserver: DataObserver,
    private val timeProvider: TimeProvider,
) {
    private val oneAtATime = Mutex()

    suspend fun capture(posted: PostedNotification) {
        oneAtATime.withLock {
            val sources = repository.findSources()
            if (sources.any { it.kind == PaymentSourceEntity.App && it.id == posted.packageName }) {
                read(posted, sources)
            }
        }
    }

    private suspend fun read(posted: PostedNotification, sources: List<PaymentSourceEntity>) {
        val reading = PaymentNotificationReader.read(
            NotificationText(posted.packageName, posted.appName, posted.title, posted.text),
        )
        val money = reading as? Reading.Money
        val paysFrom = paysFrom(sources, posted.packageName, money?.card)
        val account = paysFrom?.let { accountRepository.findById(it.account) }
        val recent = repository.findSince(posted.postTime - NotificationDecisions.Window)
        val adding = appPreferences.addFromNotifications()
        val decision = NotificationDecisions.decide(
            posted = Posted(posted.packageName, posted.key, posted.postTime, textOf(posted.title, posted.text)),
            reading = reading,
            earlier = recent.map { it.asEarlier() },
            handEntered = account?.let { handEntered(it.id, posted.postTime, recent) }.orEmpty(),
            accountCurrency = account?.asset?.code,
            adding = adding,
        )
        // While adding is off, only the log shows what was read: nothing goes to Home's list.
        if (decision is Decision.Keep) keep(posted, money, paysFrom, decision, seen = !adding)
    }

    private suspend fun keep(
        posted: PostedNotification,
        money: Reading.Money?,
        paysFrom: PaysFrom?,
        decision: Decision.Keep,
        seen: Boolean,
    ) {
        val added = if (decision.outcome == Outcome.Added && money != null && paysFrom != null) {
            transactions.add(
                NotificationPayment(
                    cents = money.cents,
                    currency = money.currency,
                    merchant = money.merchant,
                    time = posted.postTime,
                    account = paysFrom.account,
                    creditCard = paysFrom.creditCard?.takeIf { creditCardRepository.findById(it) != null },
                )
            )
        } else {
            null
        }
        // Added but not saved: its account went meanwhile.
        val unsaved = decision.outcome == Outcome.Added && added == null
        repository.save(
            PaymentNotificationEntity(
                id = UUID.randomUUID(),
                packageName = posted.packageName,
                appName = posted.appName,
                notificationKey = posted.key,
                postTime = posted.postTime,
                title = posted.title,
                text = posted.text,
                outcome = (if (unsaved) Outcome.NotAdded else decision.outcome).name,
                why = (if (unsaved) Why.NoAccount else decision.why)?.name,
                way = money?.way?.name,
                cents = money?.cents,
                currency = money?.currency,
                merchant = money?.merchant,
                card = money?.card,
                transactionId = added?.value,
                about = decision.about,
                aboutTime = decision.at,
                guessedAccount = added != null && paysFrom?.guessed == true,
                reviewed = seen,
            )
        )
        repository.deleteOlderThan(timeProvider.utcNow() - NotificationsKeptFor)
        dataObserver.post(DataWriteEvent.PaymentNotificationsChanged)
    }

    // The expenses entered by hand on the account in the hour before: not added from a notification.
    private suspend fun handEntered(
        account: AccountId,
        time: Instant,
        recent: List<PaymentNotificationEntity>,
    ): List<HandEntered> {
        val fromNotifications = recent.mapNotNull { it.transactionId }.toSet()
        return transactionRepository.findAllByAccountAndBetween(account, time - NotificationDecisions.Window, time)
            .filterIsInstance<Expense>()
            .filter { it.id.value !in fromNotifications }
            .map {
                HandEntered(
                    cents = (it.value.amount.value * CentsInUnit).roundToLong(),
                    currency = it.value.asset.code,
                    time = it.time,
                )
            }
    }

    private companion object {
        const val CentsInUnit = 100
    }
}

/** A notification's title and text as the reader sees them, to tell whether two are the same. */
fun textOf(title: String?, text: String?): String =
    listOfNotNull(title, text).joinToString("\n") { PaymentNotificationReader.normalize(it) }

/** What a kept notification says for the duplicate rules. */
fun PaymentNotificationEntity.asEarlier() = Earlier(
    id = id,
    packageName = packageName,
    key = notificationKey,
    postTime = postTime,
    text = textOf(title, text),
    cents = cents,
    currency = currency,
    card = card,
    outcome = outcomeOf(outcome),
    about = about,
)

/** An outcome by its name; an unknown one, from a later version, as ignored. */
fun outcomeOf(name: String): Outcome = Outcome.entries.find { it.name == name } ?: Outcome.Ignored

/** A reason by its name; null when there's none or it's unknown. */
fun whyOf(name: String?): Why? = Why.entries.find { it.name == name }
