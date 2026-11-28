package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.PaymentSourceDao
import com.nivy.data.db.dao.write.WritePaymentSourceDao
import com.nivy.data.db.entity.PaymentSourceEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakePaymentSourceDao : PaymentSourceDao, WritePaymentSourceDao {
    private val items = mutableListOf<PaymentSourceEntity>()

    override suspend fun findAll(): List<PaymentSourceEntity> = items.toList()

    override suspend fun save(value: PaymentSourceEntity) {
        items.upsert(value) { it.kind == value.kind && it.id == value.id }
    }

    override suspend fun saveMany(values: List<PaymentSourceEntity>) {
        values.forEach { save(it) }
    }

    override suspend fun delete(kind: String, id: String) {
        items.removeIf { it.kind == kind && it.id == id }
    }

    override suspend fun deleteAllByAccountId(accountId: UUID) {
        items.removeIf { it.accountId == accountId }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
