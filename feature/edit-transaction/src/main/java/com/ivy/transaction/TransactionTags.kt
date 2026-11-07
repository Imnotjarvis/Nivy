package com.ivy.transaction

import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.mapper.TagMapper
import com.ivy.wallet.utils.toLowerCaseLocal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/** Finds, saves and deletes tags, and links them to transactions. */
class TransactionTags @Inject constructor(
    private val tagRepository: TagRepository,
    private val tagMapper: TagMapper,
) {
    suspend fun getAllTags(): ImmutableList<Tag> =
        tagRepository.findAll().toImmutableList()

    suspend fun findByIds(ids: List<TagId>): List<Tag> = tagRepository.findByIds(ids)

    /** The ids of the tags linked to [associatedId]. */
    suspend fun associatedTagIds(associatedId: AssociationId): ImmutableList<TagId> =
        tagRepository.findByAssociatedId(associatedId).map(Tag::id).toImmutableList()

    /** Saves a tag named [name] and returns all tags, or null when the name is blank. */
    suspend fun saveTag(name: String): ImmutableList<Tag>? =
        NotBlankTrimmedString.from(name.toLowerCaseLocal())
            .map {
                val tag = with(tagMapper) { createNewTag(name = it) }
                tagRepository.save(tag)
                getAllTags()
            }
            .getOrNull()

    /** Links [selectedTag] to [associatedId] and returns the ids of its tags then. */
    suspend fun associateTag(associatedId: AssociationId, selectedTag: Tag): ImmutableList<TagId> {
        tagRepository.associateTagToEntity(associatedId, selectedTag.id)
        return associatedTagIds(associatedId)
    }

    /** Unlinks [selectedTag] from [associatedId] and returns the ids of its tags then. */
    suspend fun removeTagAssociation(associatedId: AssociationId, selectedTag: Tag): ImmutableList<TagId> {
        tagRepository.removeTagAssociation(associatedId, selectedTag.id)
        return associatedTagIds(associatedId)
    }

    /** The tags with [query] in their name, or all of them when it's blank. */
    suspend fun searchTags(query: String): ImmutableList<Tag> =
        NotBlankTrimmedString.from(query.toLowerCaseLocal()).fold(
            ifLeft = { tagRepository.findAll().toImmutableList() },
            ifRight = { tagRepository.findByText(text = it.value).toImmutableList() },
        )

    /** Deletes [selectedTag] and returns the tags left. */
    suspend fun deleteTag(selectedTag: Tag): ImmutableList<Tag> {
        tagRepository.deleteById(selectedTag.id)
        return tagRepository.findAll().toImmutableList()
    }

    /** Saves [newTag]'s changes and returns all tags. */
    suspend fun updateTag(newTag: Tag): ImmutableList<Tag> {
        tagRepository.save(newTag)
        return tagRepository.findAll().toImmutableList()
    }
}
