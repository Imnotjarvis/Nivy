package com.ivy.transactions

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Category
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.getFromAccount
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.transaction.TransactionAmountsUseCase
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.filterOverdue
import com.ivy.legacy.data.model.filterUpcoming
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import com.ivy.legacy.domain.data.TransactionHistoryItem
import java.util.UUID
import javax.inject.Inject

/** Totals, history and due transactions of one category, or of the transactions without one. */
@Suppress("TooManyFunctions")
class CategoryTransactionsLogic @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionAmountsUseCase: TransactionAmountsUseCase,
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct,
    private val timeProvider: TimeProvider,
) {
    suspend fun calculateCategoryBalance(
        category: Category,
        range: FromToTimeRange,
        accountFilterSet: Set<UUID> = emptySet(),
        transactions: List<Transaction> = emptyList()
    ): Double = transactionAmountsUseCase.balance(
        historyByCategory(category, range, accountFilterSet, transactions),
        currencyRepository.getBaseCurrency()
    )

    suspend fun calculateCategoryIncome(
        category: Category,
        range: FromToTimeRange,
        accountFilterSet: Set<UUID> = emptySet(),
    ): Double = total(
        transactionRepository.findAllByCategoryAndTypeAndBetween(
            categoryId = category.id.value,
            type = TransactionType.INCOME,
            startDate = range.from(),
            endDate = range.to()
        ).inAccounts(accountFilterSet)
    )

    suspend fun calculateCategoryIncome(
        incomeTransaction: List<Transaction>,
        accountFilterSet: Set<UUID> = emptySet()
    ): Double = total(incomeTransaction.inAccounts(accountFilterSet))

    suspend fun calculateCategoryExpenses(
        category: Category,
        range: FromToTimeRange,
        accountFilterSet: Set<UUID> = emptySet(),
    ): Double = total(
        transactionRepository.findAllByCategoryAndTypeAndBetween(
            categoryId = category.id.value,
            type = TransactionType.EXPENSE,
            startDate = range.from(),
            endDate = range.to()
        ).inAccounts(accountFilterSet)
    )

    suspend fun calculateCategoryExpenses(
        expenseTransactions: List<Transaction>,
        accountFilterSet: Set<UUID> = emptySet()
    ): Double = total(expenseTransactions.inAccounts(accountFilterSet))

    suspend fun calculateUnspecifiedBalance(range: FromToTimeRange): Double =
        calculateUnspecifiedIncome(range) - calculateUnspecifiedExpenses(range)

    suspend fun calculateUnspecifiedIncome(range: FromToTimeRange): Double = total(
        transactionRepository.findAllUnspecifiedAndTypeAndBetween(
            type = TransactionType.INCOME,
            startDate = range.from(),
            endDate = range.to()
        )
    )

    suspend fun calculateUnspecifiedExpenses(range: FromToTimeRange): Double = total(
        transactionRepository.findAllUnspecifiedAndTypeAndBetween(
            type = TransactionType.EXPENSE,
            startDate = range.from(),
            endDate = range.to()
        )
    )

    suspend fun historyByCategoryAccountWithDateDividers(
        category: Category,
        range: FromToTimeRange,
        accountFilterSet: Set<UUID>,
        transactions: List<Transaction> = emptyList()
    ): List<TransactionHistoryItem> = withDateDividers(
        historyByCategory(category, range, accountFilterSet, transactions)
    )

    suspend fun historyUnspecified(range: FromToTimeRange): List<TransactionHistoryItem> =
        withDateDividers(
            transactionRepository.findAllUnspecifiedAndBetween(
                startDate = range.from(),
                endDate = range.to()
            )
        )

    suspend fun calculateUpcomingIncomeByCategory(category: Category, range: FromToTimeRange): Double =
        total(upcomingByCategory(category, range).filterIsInstance<Income>())

    suspend fun calculateUpcomingExpensesByCategory(category: Category, range: FromToTimeRange): Double =
        total(upcomingByCategory(category, range).filterIsInstance<Expense>())

    suspend fun calculateUpcomingIncomeUnspecified(range: FromToTimeRange): Double =
        total(upcomingUnspecified(range).filterIsInstance<Income>())

    suspend fun calculateUpcomingExpensesUnspecified(range: FromToTimeRange): Double =
        total(upcomingUnspecified(range).filterIsInstance<Expense>())

    suspend fun upcomingByCategory(category: Category, range: FromToTimeRange): List<Transaction> =
        transactionRepository.findAllDueToBetweenByCategory(
            categoryId = category.id,
            startDate = range.upcomingFrom(timeProvider),
            endDate = range.to()
        ).filterUpcoming(timeProvider)

    suspend fun upcomingUnspecified(range: FromToTimeRange): List<Transaction> =
        transactionRepository.findAllDueToBetweenByCategoryUnspecified(
            startDate = range.upcomingFrom(timeProvider),
            endDate = range.to()
        ).filterUpcoming(timeProvider)

    suspend fun calculateOverdueIncomeByCategory(category: Category, range: FromToTimeRange): Double =
        total(overdueByCategory(category, range).filterIsInstance<Income>())

    suspend fun calculateOverdueExpensesByCategory(category: Category, range: FromToTimeRange): Double =
        total(overdueByCategory(category, range).filterIsInstance<Expense>())

    suspend fun calculateOverdueIncomeUnspecified(range: FromToTimeRange): Double =
        total(overdueUnspecified(range).filterIsInstance<Income>())

    suspend fun calculateOverdueExpensesUnspecified(range: FromToTimeRange): Double =
        total(overdueUnspecified(range).filterIsInstance<Expense>())

    suspend fun overdueByCategory(category: Category, range: FromToTimeRange): List<Transaction> =
        transactionRepository.findAllDueToBetweenByCategory(
            categoryId = category.id,
            startDate = range.from(),
            endDate = range.overdueTo(timeProvider)
        ).filterOverdue(timeProvider)

    suspend fun overdueUnspecified(range: FromToTimeRange): List<Transaction> =
        transactionRepository.findAllDueToBetweenByCategoryUnspecified(
            startDate = range.from(),
            endDate = range.overdueTo(timeProvider)
        ).filterOverdue(timeProvider)

    private suspend fun historyByCategory(
        category: Category,
        range: FromToTimeRange,
        accountFilterSet: Set<UUID>,
        transactions: List<Transaction>
    ): List<Transaction> = transactions.ifEmpty {
        transactionRepository.findAllByCategoryAndBetween(
            categoryId = category.id.value,
            startDate = range.from(),
            endDate = range.to()
        )
    }.inAccounts(accountFilterSet)

    /** The history only shows what already happened. */
    private suspend fun withDateDividers(transactions: List<Transaction>): List<TransactionHistoryItem> =
        trnsWithDateDivsAct(
            TrnsWithDateDivsAct.Input(
                baseCurrency = currencyRepository.getBaseCurrency().code,
                transactions = transactions.filter { it.settled }
            )
        )

    private suspend fun total(transactions: List<Transaction>): Double =
        transactionAmountsUseCase.total(transactions, currencyRepository.getBaseCurrency())

    private fun List<Transaction>.inAccounts(accountFilterSet: Set<UUID>): List<Transaction> =
        filter { accountFilterSet.isEmpty() || it.getFromAccount().value in accountFilterSet }
}
