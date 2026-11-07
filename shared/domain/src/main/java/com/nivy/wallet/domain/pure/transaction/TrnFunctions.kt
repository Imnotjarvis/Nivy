package com.nivy.wallet.domain.pure.transaction

import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
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
