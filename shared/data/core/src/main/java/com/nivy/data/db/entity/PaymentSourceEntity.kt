package com.nivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import com.nivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Where a payment notification's money comes from: an app picked to read, by its package name,
 * or a card seen in the notifications, by its last four digits, and the account it pays from, or
 * that account's [creditCardId].
 */
@Keep
@Serializable
@Entity(tableName = "payment_sources", primaryKeys = ["kind", "id"])
data class PaymentSourceEntity(
    @SerialName("kind")
    val kind: String,
    // The app's package name, or the card's last four digits.
    @SerialName("id")
    val id: String,
    // The app's name when it was picked, shown while it can't be read from the phone.
    @SerialName("name")
    val name: String?,
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("creditCardId")
    @Serializable(with = KSerializerUUID::class)
    val creditCardId: UUID? = null,
) {
    companion object {
        const val App = "app"
        const val Card = "card"
    }
}
