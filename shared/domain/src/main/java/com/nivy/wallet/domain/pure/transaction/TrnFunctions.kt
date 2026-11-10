package com.nivy.wallet.domain.pure.transaction

import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import java.time.Instant
import java.time.LocalDate

fun expenses(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Expense>()
}

fun incomes(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Income>()
}

// Due today counts as upcoming, earlier days as overdue: by date for Home's due payments, by
// time for the others.
fun isUpcoming(dueDate: LocalDate, dateNow: LocalDate): Boolean {
    return dateNow.isBefore(dueDate) || dateNow.isEqual(dueDate)
}

fun isOverdue(dueDate: LocalDate, dateNow: LocalDate): Boolean {
    return dateNow.isAfter(dueDate)
}

fun startOfToday(timeProvider: TimeProvider): Instant =
    timeProvider.localDateNow().atStartOfDay(timeProvider.getZoneId()).toInstant()

fun Iterable<Transaction>.filterUpcoming(
    timeProvider: TimeProvider,
): List<Transaction> {
    val startOfToday = startOfToday(timeProvider)
    return filter { !it.settled && !it.time.isBefore(startOfToday) }
}

fun Iterable<Transaction>.filterOverdue(
    timeProvider: TimeProvider,
): List<Transaction> {
    val startOfToday = startOfToday(timeProvider)
    return filter { !it.settled && it.time.isBefore(startOfToday) }
}
