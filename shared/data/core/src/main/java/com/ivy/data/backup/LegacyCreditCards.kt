package com.ivy.data.backup

import androidx.annotation.Keep
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import com.ivy.base.model.AccountType
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.CreditCardEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/** The accounts of a backup from before credit cards had their own table, with their card. */
@Keep
@Serializable
data class LegacyCardAccounts(
    @SerialName("accounts")
    val accounts: List<LegacyCardAccount> = emptyList(),
)

@Keep
@Serializable
data class LegacyCardAccount(
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: AccountType = AccountType.OTHER,
    @SerialName("creditLimit")
    val creditLimit: Double? = null,
    @SerialName("chargeDay")
    val chargeDay: Int? = null,
    @SerialName("statementClosingDay")
    val statementClosingDay: Int? = null,
)

/**
 * The backup with the cards of its credit card accounts, when it's from before credit cards had
 * their own table: as the database migration does, each card takes its account's id, and the
 * account's expenses and planned expenses are paid with it.
 */
fun IvyWalletCompleteData.withLegacyCards(legacy: List<LegacyCardAccount>): IvyWalletCompleteData {
    if (creditCards.isNotEmpty()) return this
    val accountIds = accounts.map { it.id }.toSet()
    val cards = legacy.mapNotNull { account ->
        val chargeDay = account.chargeDay
        if (account.type != AccountType.CREDIT_CARD || chargeDay == null || account.id !in accountIds) {
            return@mapNotNull null
        }
        CreditCardEntity(
            accountId = account.id,
            name = account.name,
            chargeDay = chargeDay,
            statementClosingDay = account.statementClosingDay,
            creditLimit = account.creditLimit,
            id = account.id,
        )
    }
    if (cards.isEmpty()) return this
    val cardAccounts = cards.map { it.accountId }.toSet()
    return copy(
        creditCards = cards,
        transactions = transactions.map {
            if (it.type == TransactionType.EXPENSE && it.accountId in cardAccounts) {
                it.copy(creditCardId = it.accountId)
            } else {
                it
            }
        },
        plannedPaymentRules = plannedPaymentRules.map {
            if (it.type == TransactionType.EXPENSE && it.accountId in cardAccounts) {
                it.copy(creditCardId = it.accountId)
            } else {
                it
            }
        },
    )
}
