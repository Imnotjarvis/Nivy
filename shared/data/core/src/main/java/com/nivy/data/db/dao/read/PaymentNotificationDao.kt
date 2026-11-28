package com.nivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.nivy.data.db.entity.PaymentNotificationEntity
import java.time.Instant
import java.util.UUID

@Dao
interface PaymentNotificationDao {
    /** The newest first. */
    @Query("SELECT * FROM payment_notifications WHERE postTime >= :since ORDER BY postTime DESC")
    suspend fun findSince(since: Instant): List<PaymentNotificationEntity>

    /** The newest first. */
    @Query("SELECT * FROM payment_notifications WHERE reviewed = 0 ORDER BY postTime DESC")
    suspend fun findUnreviewed(): List<PaymentNotificationEntity>

    @Query("SELECT * FROM payment_notifications WHERE id = :id")
    suspend fun findById(id: UUID): PaymentNotificationEntity?
}
