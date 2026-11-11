package com.nivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nivy.base.kotlinxserilzation.KSerializerInstant
import com.nivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Keep
@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID,

    @SerialName("name")
    val name: String,

    @Deprecated(UnusedTagColumn)
    @SerialName("description")
    val description: String?,
    @Deprecated(UnusedTagColumn)
    @SerialName("color")
    val color: Int,
    @Deprecated(UnusedTagColumn)
    @SerialName("icon")
    val icon: String?,
    @Deprecated(UnusedTagColumn)
    @SerialName("orderNum")
    val orderNum: Double,

    @SerialName("creationTime")
    @Serializable(with = KSerializerInstant::class)
    val dateTime: Instant,

    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isDeleted")
    val isDeleted: Boolean,

    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("lastSyncTime")
    @Serializable(with = KSerializerInstant::class)
    val lastSyncedTime: Instant
)

private const val UnusedTagColumn =
    "Never set or read: every tag has the same. Kept for the backups and databases that have it"
