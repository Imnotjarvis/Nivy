package com.nivy.wallet.domain.pure.transaction

import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import java.time.Instant

fun expenses(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Expense>()
}

fun incomes(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Income>()
}

fun transfers(transactions: List<Transaction>): List<Transaction> {
    return transactions.filterIsInstance<Transfer>()
}

// Due today counts as upcoming, earlier days as overdue.
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
