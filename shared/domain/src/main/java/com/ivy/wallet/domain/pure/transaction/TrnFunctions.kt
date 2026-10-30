package com.ivy.wallet.domain.pure.transaction

import arrow.core.Option
import arrow.core.toOption
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.temp.migration.getAccountId
import com.ivy.frp.Pure
import com.ivy.wallet.domain.pure.account.accountCurrency
import java.time.LocalDate

@Pure
fun expenses(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Expense>()
}

@Pure
fun incomes(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Income>()
}

@Pure
fun transfers(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Transfer>()
}

@Pure
fun isUpcoming(dueDate: LocalDate, dateNow: LocalDate): Boolean {
    return dateNow.isBefore(dueDate) || dateNow.isEqual(dueDate)
}

@Pure
fun isOverdue(dueDate: LocalDate, dateNow: LocalDate): Boolean {
    return dateNow.isAfter(dueDate)
}

@Pure
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
