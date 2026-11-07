package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.AccountValueDao
import com.nivy.data.db.dao.write.WriteAccountValueDao
import com.nivy.data.db.entity.AccountValueEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeAccountValueDao : AccountValueDao, WriteAccountValueDao {
    private val items = mutableListOf<AccountValueEntity>()

    override suspend fun findAll(): List<AccountValueEntity> = items.sortedBy { it.time }

    override suspend fun findByAccountId(accountId: UUID): List<AccountValueEntity> =
        findAll().filter { it.accountId == accountId }

    override suspend fun save(value: AccountValueEntity) {
        items.removeIf { it.id == value.id }
        items.add(value)
    }

    override suspend fun saveMany(values: List<AccountValueEntity>) {
        values.forEach { save(it) }
    }

    override suspend fun deleteById(id: UUID) {
        items.removeIf { it.id == id }
    }

    override suspend fun deleteAllByAccountId(accountId: UUID) {
        items.removeIf { it.accountId == accountId }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
