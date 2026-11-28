package com.nivy.domain.notification

import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.TransactionId
import com.nivy.data.repository.PaymentNotificationRepository
import java.util.UUID
import javax.inject.Inject

/**
 * What a person does with the payments added from notifications, and with what wasn't added:
 * Undo, Add back, Add, Separate, Dismiss, All good; and which account an app or a card pays from.
 */
class PaymentNotificationActions @Inject constructor(
    private val repository: PaymentNotificationRepository,
    private val transactions: NotificationTransactions,
    private val dataObserver: DataObserver,
) {
    /** Deletes the expense a notification added; it stays undone, its repeats and copies too. */
    suspend fun undo(id: UUID) = update(id, Outcome.Added) { notification ->
        notification.transactionId?.let { transactions.remove(TransactionId(it)) }
        notification.copy(outcome = Outcome.Undone.name, transactionId = null)
    }

    /** Adds again the expense that was undone. */
    suspend fun addBack(id: UUID) = update(id, Outcome.Undone) { added(it) }

    /**
     * Adds what wasn't: money received as an income, anything else as an expense; or a second
     * app's notification of a payment as a payment of its own.
     */
    suspend fun add(id: UUID) = update(id, Outcome.NotAdded, Outcome.SamePayment) { added(it) }

    /** Puts away a notification that wasn't added. */
    suspend fun dismiss(id: UUID) = update(id, Outcome.NotAdded) {
        it.copy(outcome = Outcome.Dismissed.name, reviewed = true)
    }

    /** Everything in the list was seen: it leaves the list and Home. */
    suspend fun allGood() {
        repository.markAllReviewed()
        dataObserver.post(DataWriteEvent.PaymentNotificationsChanged)
    }

    /**
     * The account [card] pays from, or that account's [creditCard]. The expenses its notifications
     * added to a guessed account move to it.
     */
    suspend fun setCardAccount(card: String, account: AccountId, creditCard: CreditCardId?) {
        repository.saveSource(
            PaymentSourceEntity(
                kind = PaymentSourceEntity.Card,
                id = card,
                name = null,
                accountId = account.value,
                creditCardId = creditCard?.value,
            )
        )
        repository.findUnreviewed()
            .filter { it.card == card && it.guessedAccount && outcomeOf(it.outcome) == Outcome.Added }
            .forEach { notification ->
                notification.transactionId?.let { transactions.remove(TransactionId(it)) }
                repository.save(added(notification).copy(guessedAccount = false))
            }
        dataObserver.post(DataWriteEvent.PaymentNotificationsChanged)
    }

    /** The account the app [packageName] pays from, or that account's [creditCard]. */
    suspend fun setAppAccount(packageName: String, appName: String, account: AccountId, creditCard: CreditCardId?) {
        repository.saveSource(
            PaymentSourceEntity(
                kind = PaymentSourceEntity.App,
                id = packageName,
                name = appName,
                accountId = account.value,
                creditCardId = creditCard?.value,
            )
        )
        dataObserver.post(DataWriteEvent.PaymentNotificationsChanged)
    }

    /** Stops reading the app [packageName], or forgets [card]'s account. */
    suspend fun remove(kind: String, id: String) {
        repository.deleteSource(kind, id)
        dataObserver.post(DataWriteEvent.PaymentNotificationsChanged)
    }

    // The notification's payment added, on the account its card or its app pays from; unchanged
    // when it can't be: its app isn't picked any more, or its account is gone.
    private suspend fun added(notification: PaymentNotificationEntity): PaymentNotificationEntity {
        val paysFrom = paysFrom(repository.findSources(), notification.packageName, notification.card)
        val cents = notification.cents
        val currency = notification.currency
        val id = if (paysFrom != null && cents != null && currency != null) {
            transactions.add(
                NotificationPayment(
                    cents = cents,
                    currency = currency,
                    merchant = notification.merchant,
                    time = notification.postTime,
                    account = paysFrom.account,
                    creditCard = paysFrom.creditCard,
                    income = notification.way == MoneyWay.Received.name,
                )
            )
        } else {
            null
        }
        return if (id == null) {
            notification
        } else {
            notification.copy(
                outcome = Outcome.Added.name,
                transactionId = id.value,
                guessedAccount = paysFrom?.guessed == true,
            )
        }
    }

    // Changes the notification [id] while it's one of [from], and tells Home and the list.
    private suspend fun update(
        id: UUID,
        vararg from: Outcome,
        change: suspend (PaymentNotificationEntity) -> PaymentNotificationEntity,
    ) {
        val notification = repository.findById(id)?.takeIf { outcomeOf(it.outcome) in from }
        if (notification != null) {
            repository.save(change(notification))
            dataObserver.post(DataWriteEvent.PaymentNotificationsChanged)
        }
    }
}
