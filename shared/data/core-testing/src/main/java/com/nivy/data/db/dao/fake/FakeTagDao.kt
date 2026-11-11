package com.nivy.data.db.dao.fake

import com.nivy.data.db.dao.read.TagAssociationDao
import com.nivy.data.db.dao.read.TagDao
import com.nivy.data.db.dao.write.WriteTagDao
import com.nivy.data.db.entity.TagEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

/**
 * @param associations the links there are between tags and what they're on, which the joins look in
 * as the database's queries do: none unless given.
 */
@VisibleForTesting
class FakeTagDao(
    private val associations: TagAssociationDao = FakeTagAssociationDao(),
) : TagDao, WriteTagDao {
    private val items = mutableListOf<TagEntity>()
    override suspend fun findAll(): List<TagEntity> {
        return items.toList()
    }

    override suspend fun findByIds(id: UUID): TagEntity? {
        return items.find { it.id == id }
    }

    override suspend fun findByText(text: String): List<TagEntity> {
        val like = likeRegex("%$text%")
        return items.filter { like.matches(it.name) }
    }

    override suspend fun findTagsByAssociatedIds(ids: List<UUID>): Map<UUID, List<TagEntity>> {
        return associations.findAll()
            .filter { it.associatedId in ids }
            .mapNotNull { link -> findByIds(link.tagId)?.let { link.associatedId to it } }
            .groupBy({ it.first }, { it.second })
    }

    override suspend fun findTagsByAssociatedId(id: UUID): List<TagEntity> {
        return findTagsByAssociatedIds(listOf(id))[id].orEmpty()
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
