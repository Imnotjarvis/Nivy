package com.nivy.domain.usecase.creditcard

import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.CreditCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** The cards of the account an expense or a transfer can be paid with, in every editor. */
fun accountCreditCards(
    transactionType: TransactionType,
    allCreditCards: List<CreditCard>,
    account: Account?,
): ImmutableList<CreditCard> =
    if (transactionType != TransactionType.INCOME) {
        allCreditCards.filter { it.account == account?.id }.toImmutableList()
    } else {
        persistentListOf()
    }

/** What a new expense on [account] is paid with: a credit card account's own card. */
fun defaultCreditCard(
    allCreditCards: List<CreditCard>,
    account: Account,
    type: TransactionType,
): CreditCard? =
    allCreditCards.firstOrNull { it.account == account.id }
        ?.takeIf { type == TransactionType.EXPENSE && account.type == AccountType.CREDIT_CARD }
