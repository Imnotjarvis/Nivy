package com.ivy.transactions

import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.transaction.TransactionAmountsUseCase
import com.ivy.wallet.data.model.FromToTimeRange
import com.ivy.wallet.data.model.filterOverdue
import com.ivy.wallet.data.model.filterUpcoming
import javax.inject.Inject

/** Due transactions of one account, with totals in the base currency as the screen labels them. */
class AccountTransactionsLogic @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionAmountsUseCase: TransactionAmountsUseCase,
    private val timeProvider: TimeProvider
) {

    suspend fun calculateUpcomingIncome(
        account: Account,
        range: FromToTimeRange
    ): Double =
        upcoming(account, range = range)
            .filterIsInstance<Income>()
            .let { total(it) }

    suspend fun calculateUpcomingExpenses(
        account: Account,
        range: FromToTimeRange
    ): Double =
        upcoming(account = account, range = range)
            .filterIsInstance<Expense>()
            .let { total(it) }

    suspend fun calculateOverdueIncome(
        account: Account,
        range: FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Income>()
            .let { total(it) }

    suspend fun calculateOverdueExpenses(
        account: Account,
        range: FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Expense>()
            .let { total(it) }

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

    private suspend fun total(transactions: List<com.ivy.data.model.Transaction>): Double =
        transactionAmountsUseCase.total(transactions, currencyRepository.getBaseCurrency())
}
