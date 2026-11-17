package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.QuickAddDao
import com.nivy.data.db.dao.write.WriteQuickAddDao
import com.nivy.data.db.entity.QuickAddEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeQuickAddDao : QuickAddDao, WriteQuickAddDao {
    private val items = mutableListOf<QuickAddEntity>()

    override suspend fun findAll(): List<QuickAddEntity> = items.sortedBy { it.orderNum }

    override suspend fun findById(id: UUID): QuickAddEntity? = items.find { it.id == id }

    override suspend fun save(value: QuickAddEntity) {
        items.upsert(value) { it.id == value.id }
    }

    override suspend fun saveMany(values: List<QuickAddEntity>) {
        values.forEach { save(it) }
    }

    override suspend fun deleteAllByAccountId(accountId: UUID) {
        items.removeIf { it.accountId == accountId || it.toAccountId == accountId }
    }

    override suspend fun deleteById(id: UUID) {
        items.removeIf { it.id == id }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
