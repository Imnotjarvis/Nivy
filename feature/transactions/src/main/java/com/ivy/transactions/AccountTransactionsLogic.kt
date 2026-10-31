package com.ivy.transactions

import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.temp.migration.getValue
import com.ivy.wallet.data.model.FromToTimeRange
import com.ivy.wallet.data.model.filterOverdue
import com.ivy.wallet.data.model.filterUpcoming
import javax.inject.Inject

/** Due transactions of one account, in its currency. */
class AccountTransactionsLogic @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider
) {

    suspend fun calculateUpcomingIncome(
        account: Account,
        range: FromToTimeRange
    ): Double =
        upcoming(account, range = range)
            .filterIsInstance<Income>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateUpcomingExpenses(
        account: Account,
        range: FromToTimeRange
    ): Double =
        upcoming(account = account, range = range)
            .filterIsInstance<Expense>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateOverdueIncome(
        account: Account,
        range: FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Income>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateOverdueExpenses(
        account: Account,
        range: FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Expense>()
            .sumOf { it.getValue().toDouble() }

    suspend fun upcoming(
        account: Account,
        range: FromToTimeRange
    ): List<com.ivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = account.id,
            startDate = range.upcomingFrom(timeProvider),
            endDate = range.to()
        ).filterUpcoming(timeProvider)
    }

    suspend fun overdue(
        account: Account,
        range: FromToTimeRange
    ): List<com.ivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = account.id,
            startDate = range.from(),
            endDate = range.overdueTo(timeProvider)
        ).filterOverdue(timeProvider)
    }
}
