package com.nivy.data.model

import androidx.compose.runtime.Immutable
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import java.time.Instant
import java.util.UUID

data class Tag(
    override val id: TagId,
    val name: NotBlankTrimmedString,
    val creationTimestamp: Instant,
) : Identifiable<TagId>

data class TagAssociation(
    override val id: TagId,
    val associatedId: AssociationId,
) : Identifiable<TagId>

@JvmInline
@Immutable
value class TagId(override val value: UUID) : UniqueId
