package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.PaymentNotificationDao
import com.nivy.data.db.dao.read.PaymentSourceDao
import com.nivy.data.db.dao.write.WritePaymentNotificationDao
import com.nivy.data.db.dao.write.WritePaymentSourceDao
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.model.AccountId
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** The payment notifications Nivy read, and the apps and cards their money comes from. */
class PaymentNotificationRepository @Inject constructor(
    private val notificationDao: PaymentNotificationDao,
    private val writeNotificationDao: WritePaymentNotificationDao,
    private val sourceDao: PaymentSourceDao,
    private val writeSourceDao: WritePaymentSourceDao,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** The newest first. */
    suspend fun findSince(since: Instant): List<PaymentNotificationEntity> = withContext(dispatchersProvider.io) {
        notificationDao.findSince(since)
    }

    /** The newest first. */
    suspend fun findUnreviewed(): List<PaymentNotificationEntity> = withContext(dispatchersProvider.io) {
        notificationDao.findUnreviewed()
    }

    suspend fun findById(id: UUID): PaymentNotificationEntity? = withContext(dispatchersProvider.io) {
        notificationDao.findById(id)
    }

    suspend fun save(value: PaymentNotificationEntity): Unit = withContext(dispatchersProvider.io) {
        writeNotificationDao.save(value)
    }

    suspend fun markAllReviewed(): Unit = withContext(dispatchersProvider.io) {
        writeNotificationDao.markAllReviewed()
    }

    suspend fun deleteOlderThan(before: Instant): Unit = withContext(dispatchersProvider.io) {
        writeNotificationDao.deleteOlderThan(before)
    }

    suspend fun findSources(): List<PaymentSourceEntity> = withContext(dispatchersProvider.io) {
        sourceDao.findAll()
    }

    suspend fun saveSource(value: PaymentSourceEntity): Unit = withContext(dispatchersProvider.io) {
        writeSourceDao.save(value)
    }

    suspend fun deleteSource(kind: String, id: String): Unit = withContext(dispatchersProvider.io) {
        writeSourceDao.delete(kind, id)
    }

    /** An account's apps and cards, when it's deleted. */
    suspend fun deleteSourcesByAccountId(accountId: AccountId): Unit = withContext(dispatchersProvider.io) {
        writeSourceDao.deleteAllByAccountId(accountId.value)
    }

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeNotificationDao.deleteAll()
        writeSourceDao.deleteAll()
    }
}
