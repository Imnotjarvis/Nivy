package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A credit card the account pays: the day of the month it charges the purchases, the day its
 * statement closes (null for the month's last day) and its limit.
 */
@Keep
@Serializable
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("name")
    val name: String,
    @SerialName("chargeDay")
    val chargeDay: Int,
    @SerialName("statementClosingDay")
    val statementClosingDay: Int?,
    @SerialName("creditLimit")
    val creditLimit: Double?,

    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID,
)
