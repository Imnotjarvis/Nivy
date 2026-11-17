package com.nivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nivy.base.kotlinxserilzation.KSerializerUUID
import com.nivy.base.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Keep
@Serializable
@Entity(tableName = "quick_adds")
data class QuickAddEntity(
    @SerialName("type")
    val type: TransactionType,
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    // Null when the amount is entered each time.
    @SerialName("amount")
    val amount: Double? = null,
    @SerialName("categoryId")
    @Serializable(with = KSerializerUUID::class)
    val categoryId: UUID? = null,
    @SerialName("title")
    val title: String? = null,
    // The credit card an expense or a transfer is paid with; null when it's paid from the account.
    @SerialName("creditCardId")
    @Serializable(with = KSerializerUUID::class)
    val creditCardId: UUID? = null,
    // The account a transfer goes to; null for an income or an expense.
    @SerialName("toAccountId")
    @Serializable(with = KSerializerUUID::class)
    val toAccountId: UUID? = null,
    @SerialName("orderNum")
    val orderNum: Double = 0.0,
    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID = UUID.randomUUID(),
)
