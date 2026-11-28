package com.nivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nivy.data.db.entity.PaymentNotificationEntity
import java.time.Instant

@Dao
interface WritePaymentNotificationDao {
    @Upsert
    suspend fun save(value: PaymentNotificationEntity)

    @Query("UPDATE payment_notifications SET reviewed = 1 WHERE reviewed = 0")
    suspend fun markAllReviewed()

    @Query("DELETE FROM payment_notifications WHERE postTime < :before")
    suspend fun deleteOlderThan(before: Instant)

    @Query("DELETE FROM payment_notifications")
    suspend fun deleteAll()
}
