package com.nivy.data.model.primitive

import com.nivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class AssociationId(override val value: UUID) : UniqueId
