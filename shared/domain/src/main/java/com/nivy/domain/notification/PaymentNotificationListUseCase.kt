package com.nivy.domain.notification

import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.data.repository.TransactionRepository
import java.time.Instant
import javax.inject.Inject

/**
 * A notification in the list: what became of it and why, the transaction it added, the other
 * apps that reported the same payment, and [since], the time of the one it repeats or of the
 * payment entered by hand like it.
 */
data class ListedNotification(
    val notification: PaymentNotificationEntity,
    val outcome: Outcome,
    val why: Why?,
    val transaction: Transaction?,
    val alsoIn: List<String>,
    val since: Instant?,
)

/** The list, newest first: what was added, or undone since, and what wasn't added. */
data class NotificationList(val added: List<ListedNotification>, val notAdded: List<ListedNotification>) {
    val size: Int get() = added.size + notAdded.size

    /** The payments added and still there. */
    val stillAdded: List<ListedNotification> get() = added.filter { it.outcome == Outcome.Added }
}

/** The payments added from notifications and the notifications not added, until they're seen. */
class PaymentNotificationListUseCase @Inject constructor(
    private val repository: PaymentNotificationRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun load(): NotificationList {
        val unseen = repository.findUnreviewed()
        val transactions = transactionRepository
            .findByIds(unseen.mapNotNull { it.transactionId }.map(::TransactionId))
            .associateBy { it.id.value }
        val alsoIn = unseen
            .filter { outcomeOf(it.outcome) == Outcome.SamePayment }
            .groupBy({ it.about }, { it.appName ?: it.packageName })
        val byId = unseen.associateBy { it.id }
        val listed = unseen.map { notification ->
            val transaction = notification.transactionId?.let(transactions::get)
            val outcome = outcomeOf(notification.outcome)
            ListedNotification(
                notification = notification,
                // An expense deleted since, as undone.
                outcome = if (outcome == Outcome.Added && transaction == null) Outcome.Undone else outcome,
                why = whyOf(notification.why),
                transaction = transaction,
                alsoIn = alsoIn[notification.id].orEmpty(),
                since = notification.aboutTime
                    ?: notification.about?.let { about -> (byId[about] ?: repository.findById(about))?.postTime },
            )
        }
        return NotificationList(
            added = listed.filter { it.outcome == Outcome.Added || it.outcome == Outcome.Undone },
            notAdded = listed.filter { it.outcome == Outcome.NotAdded },
        )
    }
}
