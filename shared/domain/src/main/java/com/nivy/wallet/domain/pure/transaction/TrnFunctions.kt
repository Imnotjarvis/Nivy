package com.nivy.wallet.domain.pure.transaction

import arrow.core.Option
import arrow.core.toOption
import com.nivy.data.model.Account
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.temp.migration.getAccountId
import com.nivy.wallet.domain.pure.account.accountCurrency
import java.time.LocalDate

fun expenses(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Expense>()
}

fun incomes(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Income>()
}

fun transfers(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Transfer>()
}

fun isUpcoming(dueDate: LocalDate, dateNow: LocalDate): Boolean {
    return dateNow.isBefore(dueDate) || dateNow.isEqual(dueDate)
}

fun isOverdue(dueDate: LocalDate, dateNow: LocalDate): Boolean {
    return dateNow.isAfter(dueDate)
}

fun trnCurrency(
    transaction: Transaction,
    accounts: List<Account>,
    baseCurrency: String
): Option<String> {
    val account = accounts.find {
        it.id.value == transaction.getAccountId()
    }
        ?: return baseCurrency.toOption()
    return accountCurrency(account).toOption()
}
