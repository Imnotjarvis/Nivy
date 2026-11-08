package com.nivy.transactions

import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.transaction.TransactionAmountsUseCase
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.domain.pure.transaction.filterOverdue
import com.nivy.wallet.domain.pure.transaction.filterUpcoming
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
    ): List<com.nivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = account.id,
            startDate = range.upcomingFrom(timeProvider),
            endDate = range.to()
        ).filterUpcoming(timeProvider)
    }

    suspend fun overdue(
        account: Account,
        range: FromToTimeRange
    ): List<com.nivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = account.id,
            startDate = range.from(),
            endDate = range.overdueTo(timeProvider)
        ).filterOverdue(timeProvider)
    }

    private suspend fun total(transactions: List<com.nivy.data.model.Transaction>): Double =
        transactionAmountsUseCase.total(transactions, currencyRepository.getBaseCurrency())
}
