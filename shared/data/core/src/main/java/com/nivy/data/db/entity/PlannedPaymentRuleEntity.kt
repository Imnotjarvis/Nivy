package com.nivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nivy.base.kotlinxserilzation.KSerializerInstant
import com.nivy.base.kotlinxserilzation.KSerializerUUID
import com.nivy.base.model.TransactionType
import com.nivy.base.model.IntervalType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Keep
@Serializable
@Entity(tableName = "planned_payment_rules")
data class PlannedPaymentRuleEntity(
    @SerialName("startDate")
    @Serializable(with = KSerializerInstant::class)
    val startDate: Instant?,
    @SerialName("intervalN")
    val intervalN: Int?,
    @SerialName("intervalType")
    val intervalType: IntervalType?,
    @SerialName("oneTime")
    val oneTime: Boolean,
    @SerialName("type")
    val type: TransactionType,
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("amount")
    val amount: Double = 0.0,
    @SerialName("categoryId")
    @Serializable(with = KSerializerUUID::class)
    val categoryId: UUID? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    // The credit card a planned expense is paid with; null when it's paid from the account.
    @SerialName("creditCardId")
    @Serializable(with = KSerializerUUID::class)
    val creditCardId: UUID? = null,

    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isSynced")
    val isSynced: Boolean = false,
    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isDeleted")
    val isDeleted: Boolean = false,

    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID = UUID.randomUUID()
)
