package com.nivy.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A notification from an app picked for payment notifications, and what Nivy made of it: kept for
 * the log and the list of what was added, a month at most. Not in backups: it holds the
 * notifications' text.
 *
 * [outcome], [why] and [way] are the names of the reader's and the decisions' values. [about] is the
 * notification it repeats or is the same payment as; [aboutTime] the time of a payment entered by
 * hand like it. [transactionId] is the expense it added. [guessedAccount] is true when its card's
 * account wasn't known yet.
 */
@Entity(
    tableName = "payment_notifications",
    indices = [Index("postTime"), Index("reviewed")],
)
data class PaymentNotificationEntity(
    @PrimaryKey
    val id: UUID,
    val packageName: String,
    val appName: String?,
    val notificationKey: String,
    val postTime: Instant,
    val title: String?,
    val text: String?,
    val outcome: String,
    val why: String?,
    val way: String?,
    val cents: Long?,
    val currency: String?,
    val merchant: String?,
    val card: String?,
    val transactionId: UUID?,
    val about: UUID?,
    val aboutTime: Instant?,
    val guessedAccount: Boolean,
    val reviewed: Boolean,
)
