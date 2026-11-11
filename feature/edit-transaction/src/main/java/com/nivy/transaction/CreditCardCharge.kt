package com.nivy.transaction

import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.CreditCard
import com.nivy.domain.usecase.creditcard.chargeDateTime
import com.nivy.domain.usecase.creditcard.minChargeShift
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.LocalDateTime

/** The cards of the account an expense or a transfer can be paid with. */
internal fun accountCreditCards(
    transactionType: TransactionType,
    allCreditCards: List<CreditCard>,
    account: Account?,
): ImmutableList<CreditCard> =
    if (transactionType != TransactionType.INCOME) {
        allCreditCards.filter { it.account == account?.id }.toImmutableList()
    } else {
        persistentListOf()
    }

/** When the card charges the expense or transfer, if it's paid with a credit card and not planned. */
internal fun chargeDate(
    transactionType: TransactionType,
    dueDate: Instant?,
    creditCard: CreditCard?,
    chargeShift: Int,
    purchase: () -> LocalDateTime,
): LocalDateTime? {
    if (transactionType == TransactionType.INCOME || dueDate != null) return null
    return creditCard?.chargeDateTime(purchase(), chargeShift)
}

/** Whether the charge can move a month earlier without falling on or before the purchase. */
internal fun canMoveChargeEarlier(
    creditCard: CreditCard?,
    chargeShift: Int,
    purchase: () -> LocalDateTime,
): Boolean {
    val card = creditCard ?: return false
    return chargeShift > card.minChargeShift(purchase())
}

/** What a new expense on [account] is paid with: a credit card account's own card. */
internal fun defaultCreditCard(
    allCreditCards: List<CreditCard>,
    account: Account,
    type: TransactionType,
): CreditCard? =
    allCreditCards.firstOrNull { it.account == account.id }
        ?.takeIf { type == TransactionType.EXPENSE && account.type == AccountType.CREDIT_CARD }
