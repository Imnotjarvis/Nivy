package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.PaymentNotificationDao
import com.nivy.data.db.dao.write.WritePaymentNotificationDao
import com.nivy.data.db.entity.PaymentNotificationEntity
import org.jetbrains.annotations.VisibleForTesting
import java.time.Instant
import java.util.UUID

@VisibleForTesting
class FakePaymentNotificationDao : PaymentNotificationDao, WritePaymentNotificationDao {
    private val items = mutableListOf<PaymentNotificationEntity>()

    override suspend fun findSince(since: Instant): List<PaymentNotificationEntity> =
        items.filter { !it.postTime.isBefore(since) }.sortedByDescending { it.postTime }

    override suspend fun findUnreviewed(): List<PaymentNotificationEntity> =
        items.filter { !it.reviewed }.sortedByDescending { it.postTime }

    override suspend fun findById(id: UUID): PaymentNotificationEntity? = items.find { it.id == id }

    override suspend fun save(value: PaymentNotificationEntity) {
        items.upsert(value) { it.id == value.id }
    }

    override suspend fun markAllReviewed() {
        items.replaceAll { it.copy(reviewed = true) }
    }

    override suspend fun deleteOlderThan(before: Instant) {
        items.removeIf { it.postTime.isBefore(before) }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
