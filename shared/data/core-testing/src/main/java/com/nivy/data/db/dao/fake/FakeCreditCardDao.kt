package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.CreditCardDao
import com.nivy.data.db.dao.write.WriteCreditCardDao
import com.nivy.data.db.entity.CreditCardEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

/** The cards alone: it has no transactions or planned payments to unlink from a deleted card. */
@VisibleForTesting
class FakeCreditCardDao : CreditCardDao, WriteCreditCardDao {
    private val items = mutableListOf<CreditCardEntity>()

    override suspend fun findAll(): List<CreditCardEntity> = items.sortedBy { it.name.lowercase() }

    override suspend fun findById(id: UUID): CreditCardEntity? = items.find { it.id == id }

    override suspend fun findByAccountId(accountId: UUID): List<CreditCardEntity> =
        findAll().filter { it.accountId == accountId }

    override suspend fun save(value: CreditCardEntity) {
        items.removeIf { it.id == value.id }
        items.add(value)
    }

    override suspend fun saveMany(values: List<CreditCardEntity>) {
        values.forEach { save(it) }
    }

    override suspend fun unlinkTransactions(id: UUID) = Unit

    override suspend fun unlinkPlannedPaymentRules(id: UUID) = Unit

    override suspend fun deleteCreditCardRow(id: UUID) {
        items.removeIf { it.id == id }
    }

    override suspend fun deleteAllByAccountId(accountId: UUID) {
        items.removeIf { it.accountId == accountId }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
