package com.nivy.data.db.dao.fake

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.nivy.data.db.dao.read.TagDao
import com.nivy.data.db.dao.write.WriteTagDao
import com.nivy.data.db.entity.TagEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeTagDao : TagDao, WriteTagDao {
    private val items = mutableListOf<TagEntity>()
    override suspend fun findAll(): List<TagEntity> {
        return items
    }

    override suspend fun findByIds(id: UUID): TagEntity? {
        return items.find { it.id == id }
    }

    override suspend fun findByText(text: String): List<TagEntity> {
        return items.filter { it.name.contains(text.toLowerCase(Locale.current)) }
    }

    override suspend fun findTagsByAssociatedIds(ids: List<UUID>): Map<UUID, List<TagEntity>> {
        return emptyMap()
    }

    override suspend fun findTagsByAssociatedId(id: UUID): List<TagEntity> {
        return emptyList()
    }

    override suspend fun save(value: TagEntity) {
        items.upsert(value) { it.id == value.id }
    }

    override suspend fun save(value: List<TagEntity>) {
        value.forEach { save(it) }
    }

    override suspend fun deleteById(id: UUID) {
        items.removeIf { it.id == id }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
