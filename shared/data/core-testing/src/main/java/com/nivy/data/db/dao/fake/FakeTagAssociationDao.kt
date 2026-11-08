package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.TagAssociationDao
import com.nivy.data.db.dao.write.WriteTagAssociationDao
import com.nivy.data.db.entity.TagAssociationEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeTagAssociationDao : TagAssociationDao, WriteTagAssociationDao {
    private val items = mutableListOf<TagAssociationEntity>()
    override suspend fun findAll(): List<TagAssociationEntity> {
        return items
    }

    override suspend fun findByAllAssociatedIdForTagId(tagIds: List<UUID>): Map<UUID, List<TagAssociationEntity>> {
        val setOfIds = tagIds.toSet()
        return items.filter { setOfIds.contains(it.tagId) }.groupBy(
            { it.tagId },
            { it }
        )
    }

    // Keyed by the tag and what it's on, as the table is.
    override suspend fun save(value: TagAssociationEntity) {
        items.upsert(value) { it.tagId == value.tagId && it.associatedId == value.associatedId }
    }

    override suspend fun save(value: List<TagAssociationEntity>) {
        value.forEach { save(it) }
    }

    override suspend fun deleteAll() {
        items.clear()
    }

    override suspend fun deleteId(tagId: UUID, associatedId: UUID) {
        items.removeIf { it.tagId == tagId && it.associatedId == associatedId }
    }

    override suspend fun deleteAssociationsByTagId(tagId: UUID) {
        items.removeIf { it.tagId == tagId }
    }
}
