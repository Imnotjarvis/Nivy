package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerInstant
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * What an investment account was worth at a time, as the user entered it: its investments' [value]
 * and the [cash] in it that wasn't invested, if entered.
 */
@Keep
@Serializable
@Entity(tableName = "account_values")
data class AccountValueEntity(
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("time")
    @Serializable(with = KSerializerInstant::class)
    val time: Instant,
    @SerialName("value")
    val value: Double,
    @SerialName("cash")
    val cash: Double? = null,

    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID,
)
