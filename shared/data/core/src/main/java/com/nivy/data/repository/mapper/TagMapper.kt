package com.nivy.data.repository.mapper

import android.graphics.Color
import arrow.core.Either
import arrow.core.raise.either
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TagEntity
import com.nivy.data.model.Tag
import com.nivy.data.model.TagAssociation
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.TagId
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class TagMapper @Inject constructor() {
    companion object {
        fun createNewTagId(): TagId = TagId(UUID.randomUUID())
    }

    fun TagEntity.toDomain(): Either<String, Tag> = either {
        Tag(
            id = TagId(id),
            name = NotBlankTrimmedString.from(name).bind(),
            creationTimestamp = dateTime,
        )
    }

    // The description, color, icon and order are what every tag has had: nothing ever set them.
    fun Tag.toEntity(): TagEntity {
        return TagEntity(
            id = id.value,
            name = name.value,
            description = null,
            color = Color.TRANSPARENT,
            icon = null,
            orderNum = 0.0,
            dateTime = creationTimestamp,
            lastSyncedTime = Instant.EPOCH,
            isDeleted = false,
        )
    }

    fun TagAssociation.toEntity(): TagAssociationEntity {
        return TagAssociationEntity(
            tagId = id.value,
            associatedId = associatedId.value,
            lastSyncedTime = Instant.EPOCH,
            isDeleted = false,
        )
    }

    fun TagAssociationEntity.toDomain(): TagAssociation {
        return TagAssociation(
            id = TagId(tagId),
            associatedId = AssociationId(associatedId),
        )
    }

    fun createNewTag(tagId: TagId = createNewTagId(), name: NotBlankTrimmedString): Tag {
        return Tag(
            id = tagId,
            name = name,
            creationTimestamp = Instant.now(),
        )
    }

    fun createNewTagAssociation(tagId: TagId, associationId: AssociationId): TagAssociation {
        return TagAssociation(
            id = tagId,
            associatedId = associationId,
        )
    }
}
