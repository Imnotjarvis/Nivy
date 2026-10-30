package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.temp.migration.getValue
import com.ivy.legacy.data.model.filterOverdue
import com.ivy.legacy.data.model.filterUpcoming
import javax.inject.Inject

@Deprecated("Migrate to FP Style")
class WalletAccountLogic @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider
) {

    suspend fun calculateUpcomingIncome(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        upcoming(account, range = range)
            .filterIsInstance<Income>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateUpcomingExpenses(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        upcoming(account = account, range = range)
            .filterIsInstance<Expense>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateOverdueIncome(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Income>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateOverdueExpenses(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Expense>()
            .sumOf { it.getValue().toDouble() }

    suspend fun upcoming(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): List<com.ivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = account.id,
            startDate = range.upcomingFrom(timeProvider),
            endDate = range.to()
        ).filterUpcoming(timeProvider)
    }

    suspend fun overdue(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): List<com.ivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = account.id,
            startDate = range.from(),
            endDate = range.overdueTo(timeProvider)
        ).filterOverdue(timeProvider)
    }
}
