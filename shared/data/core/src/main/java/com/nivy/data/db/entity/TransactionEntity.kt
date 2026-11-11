package com.nivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nivy.base.kotlinxserilzation.KSerializerInstant
import com.nivy.base.kotlinxserilzation.KSerializerUUID
import com.nivy.base.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Keep
@Serializable
@Entity(
    tableName = "transactions",
    // The columns the queries filter on: every balance and statistics query would otherwise
    // read the whole table, once per account.
    indices = [
        Index("accountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("dateTime"),
        Index("dueDate"),
        Index("recurringRuleId"),
        Index("loanId"),
        Index("loanRecordId"),
    ],
)
data class TransactionEntity(
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("type")
    val type: TransactionType,
    @SerialName("amount")
    val amount: Double,
    @SerialName("toAccountId")
    @Serializable(with = KSerializerUUID::class)
    val toAccountId: UUID? = null,
    @SerialName("toAmount")
    val toAmount: Double? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("dateTime")
    @Serializable(with = KSerializerInstant::class)
    val dateTime: Instant? = null,
    @SerialName("categoryId")
    @Serializable(with = KSerializerUUID::class)
    val categoryId: UUID? = null,
    @SerialName("dueDate")
    @Serializable(with = KSerializerInstant::class)
    val dueDate: Instant? = null,
    @SerialName("recurringRuleId")
    @Serializable(with = KSerializerUUID::class)
    val recurringRuleId: UUID? = null,
    @SerialName("paidForDateTime")
    @Serializable(with = KSerializerInstant::class)
    val paidForDateTime: Instant? = null,
    @Deprecated("Left from sync: nothing reads it. The column and its backup key stay, for the backups that have it")
    @SerialName("attachmentUrl")
    val attachmentUrl: String? = null,
    // This refers to the loan id that is linked with a transaction
    @SerialName("loanId")
    @Serializable(with = KSerializerUUID::class)
    val loanId: UUID? = null,
    // This refers to the loan record id that is linked with a transaction
    @SerialName("loanRecordId")
    @Serializable(with = KSerializerUUID::class)
    val loanRecordId: UUID? = null,
    // When a credit card expense was bought; dateTime is then the date the card charges it.
    @SerialName("purchaseDateTime")
    @Serializable(with = KSerializerInstant::class)
    val purchaseDateTime: Instant? = null,
    // The months the user moved a credit card expense's charge from its usual date.
    @SerialName("chargeShift")
    @ColumnInfo(defaultValue = "0")
    val chargeShift: Int = 0,
    // The credit card an expense is paid with; null when it's paid from the account.
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
