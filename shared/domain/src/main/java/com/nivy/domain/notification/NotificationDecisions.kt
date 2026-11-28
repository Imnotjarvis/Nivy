package com.nivy.domain.notification

import java.time.Duration
import java.time.Instant
import java.util.UUID

/** What became of a notification Nivy read. */
enum class Outcome {
    /** Added as an expense. */
    Added,

    /** Would have been added, while adding is off. */
    WouldAdd,

    /** The same payment as another app's notification, already added or would be. */
    SamePayment,

    /** Money, but not added: see its [Why]. */
    NotAdded,

    /** Not money, or a payment that was undone. */
    Ignored,

    /** Added, then taken back. */
    Undone,

    /** Not added, and put away. */
    Dismissed,
}

/** Why a notification wasn't added. */
enum class Why {
    Code, Hold, Declined, LowBalance, NotYet, Offer, Access, NoAmount,

    /** The same payment as one that was undone. */
    UndoneBefore,
    MoneyIn,
    SentToPerson,
    Cash,

    /** In another currency than the account's. */
    OtherCurrency,

    /** An amount, but nothing says which way it went. */
    Unread,

    /** The same text as another of the app's within the hour. */
    LikelyRepeat,

    /** A payment of the same amount was entered by hand in the hour before. */
    AddedByHand,

    /** The app has no account to pay from any more. */
    NoAccount,
}

/** A notification just posted: its app, Android's key for it, when it was posted, its text. */
data class Posted(val packageName: String, val key: String, val postTime: Instant, val text: String)

/** A notification read in the hour before, and what became of it. [about] is the one it repeats or matched. */
data class Earlier(
    val id: UUID,
    val packageName: String,
    val key: String,
    val postTime: Instant,
    val text: String,
    val cents: Long?,
    val currency: String?,
    val card: String?,
    val outcome: Outcome,
    val about: UUID?,
)

/** A payment entered by hand on the account a notification's would go to. */
data class HandEntered(val cents: Long, val currency: String, val time: Instant)

sealed interface Decision {
    /** The same notification again: nothing to keep. */
    data object Skip : Decision

    /** What to keep: the [outcome], [why], the earlier notification it's [about], or [at] what time. */
    data class Keep(
        val outcome: Outcome,
        val why: Why? = null,
        val about: UUID? = null,
        val at: Instant? = null,
    ) : Decision
}

/**
 * Whether a notification is a payment to add, and whether it's one already seen. Within an hour,
 * as Google Wallet's notification can trail the bank's by ten minutes and come again half an hour
 * after:
 * - the same notification again, or an update of it about the same amount, is skipped;
 * - the same text from the same app is a likely repeat, kept for a person to add;
 * - Google Wallet's notification and a bank's of the same amount are the same payment, their
 *   cards equal when both show one, the closest that hasn't been matched yet; two banks' never are;
 * - a payment that was undone keeps its repeats and Wallet's copy out;
 * - a payment of the same amount entered by hand in the hour before keeps it out too.
 */
object NotificationDecisions {
    val Window: Duration = Duration.ofHours(1)

    fun decide(
        posted: Posted,
        reading: Reading,
        earlier: List<Earlier>,
        handEntered: List<HandEntered>,
        accountCurrency: String?,
        adding: Boolean,
    ): Decision {
        val recent = earlier.filter { !it.postTime.isBefore(posted.postTime - Window) }
        if (recent.any { it.isSame(posted) }) return Decision.Skip
        return when (reading) {
            is Reading.Ignored -> Decision.Keep(Outcome.Ignored, reading.why.asWhy())
            is Reading.Money -> decideMoney(posted, reading, recent, handEntered, accountCurrency, adding)
        }
    }

    @Suppress("LongParameterList")
    private fun decideMoney(
        posted: Posted,
        money: Reading.Money,
        recent: List<Earlier>,
        handEntered: List<HandEntered>,
        accountCurrency: String?,
        adding: Boolean,
    ): Decision {
        val undone = undoneBefore(posted, money, recent)
        val repeat = recent.firstOrNull { it.repeats(posted) }
        return when {
            recent.any { it.isUpdateOf(posted, money) } -> Decision.Skip
            money.way != MoneyWay.Spent -> Decision.Keep(Outcome.NotAdded, money.way.notSpentWhy())
            undone != null -> Decision.Keep(Outcome.Ignored, Why.UndoneBefore, about = undone.id)
            repeat != null -> Decision.Keep(Outcome.NotAdded, Why.LikelyRepeat, about = repeat.id)
            else -> samePayment(posted, money, recent)
                ?.let { Decision.Keep(Outcome.SamePayment, about = it.id) }
                ?: added(money, handEntered, accountCurrency, adding)
        }
    }

    private fun added(
        money: Reading.Money,
        handEntered: List<HandEntered>,
        accountCurrency: String?,
        adding: Boolean,
    ): Decision {
        val byHand = handEntered.firstOrNull { it.cents == money.cents && it.currency == money.currency }
        return when {
            accountCurrency == null -> Decision.Keep(Outcome.NotAdded, Why.NoAccount)
            money.currency != accountCurrency -> Decision.Keep(Outcome.NotAdded, Why.OtherCurrency)
            byHand != null -> Decision.Keep(Outcome.NotAdded, Why.AddedByHand, at = byHand.time)
            else -> Decision.Keep(if (adding) Outcome.Added else Outcome.WouldAdd)
        }
    }

    // Google Wallet's notification and a bank's of the same payment, the closest in time of those
    // the other side hasn't matched yet.
    private fun samePayment(posted: Posted, money: Reading.Money, recent: List<Earlier>): Earlier? {
        val fromWallet = posted.isWallet()
        val matched = recent
            .filter { it.outcome == Outcome.SamePayment && it.isWallet() == fromWallet }
            .mapNotNull { it.about }
            .toSet()
        return recent
            .filter { it.outcome in AddedOrWould && it.isWallet() != fromWallet && it.id !in matched }
            .filter { it.sameMoney(money) }
            .minByOrNull { Duration.between(it.postTime, posted.postTime).abs() }
    }

    // An undone payment: the same text from the same app, or the other side's copy of it.
    private fun undoneBefore(posted: Posted, money: Reading.Money, recent: List<Earlier>): Earlier? =
        recent.filter { it.outcome == Outcome.Undone }.firstOrNull {
            it.sameText(posted) || it.isWallet() != posted.isWallet() && it.sameMoney(money)
        }

    private fun Earlier.isSame(posted: Posted) =
        packageName == posted.packageName && key == posted.key && postTime == posted.postTime

    // The app changing its notification: the same key, about the same amount.
    private fun Earlier.isUpdateOf(posted: Posted, money: Reading.Money) =
        sameKey(posted) && cents == money.cents && outcome in Kept

    private fun Earlier.sameKey(posted: Posted) = packageName == posted.packageName && key == posted.key

    private fun Earlier.sameText(posted: Posted) = packageName == posted.packageName && text == posted.text

    private fun Earlier.repeats(posted: Posted) = sameText(posted) && outcome in Repeatable

    private fun Earlier.sameMoney(money: Reading.Money) =
        cents == money.cents && currency == money.currency && cardsAgree(card, money.card)

    // A card tells two payments apart only when both show one.
    private fun cardsAgree(one: String?, other: String?) = one == null || other == null || one == other

    private fun Earlier.isWallet() = packageName == PaymentNotificationReader.GoogleWallet

    private fun Posted.isWallet() = packageName == PaymentNotificationReader.GoogleWallet

    private val AddedOrWould = setOf(Outcome.Added, Outcome.WouldAdd)
    private val Repeatable = setOf(Outcome.Added, Outcome.WouldAdd, Outcome.SamePayment)
    private val Kept = Repeatable + Outcome.Undone

    private fun NotMoney.asWhy(): Why = Why.valueOf(name)

    private fun MoneyWay?.notSpentWhy(): Why = when (this) {
        MoneyWay.Received -> Why.MoneyIn
        MoneyWay.Sent -> Why.SentToPerson
        MoneyWay.Cash -> Why.Cash
        else -> Why.Unread
    }
}
