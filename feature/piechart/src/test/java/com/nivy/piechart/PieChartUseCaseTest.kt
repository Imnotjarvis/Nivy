package com.nivy.piechart

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.TransactionType
import com.nivy.base.resource.ResourceProvider
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.design.system.colors.Palette
import com.nivy.domain.usecase.category.CategoryIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsInRangeUseCase
import com.nivy.piechart.usecase.PieChartUseCase
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewTransaction
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class PieChartUseCaseTest {
    private val trnsInRange = mockk<TransactionsInRangeUseCase> {
        coEvery { transactions(any(), any()) } returns emptyList()
    }
    private val useCase = PieChartUseCase(
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns persistentListOf() },
        transactionsInRangeUseCase = trnsInRange,
        transactionsIncomeExpenseUseCase = mockk<TransactionsIncomeExpenseUseCase> {
            coEvery { incomeExpense(any(), any(), any()) } returns IncomeExpenseTransferPair.zero()
        },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        categoryIncomeExpenseUseCase = mockk<CategoryIncomeExpenseUseCase> {
            coEvery { incomeExpense(any(), any(), any(), any()) } returns IncomeExpenseTransferPair.zero()
        },
        resourceProvider = mockk<ResourceProvider> { every { getString(any()) } returns "Account transfers" },
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `a budget's page charts only its transactions, even none`() = runBlocking {
        val output = pieChart(onlyExisting = true)

        output.totalAmount shouldBe 0.0
        output.categoryAmounts.shouldBeEmpty()
        coVerify(exactly = 0) { trnsInRange.transactions(any(), any()) }
    }

    @Test
    fun `without transactions given, the chart reads the month's`() = runBlocking {
        pieChart(onlyExisting = false)

        coVerify(exactly = 1) { trnsInRange.transactions(any(), any()) }
    }

    @Test
    fun `split by account, each account's expenses come largest first, without transfers or accounts with none`() =
        runBlocking<Unit> {
            val output = byAccount(TransactionType.EXPENSE)

            output.accountAmounts.map { it.account to it.amount } shouldBe listOf(Bank to 70.0, Card to 20.0)
            output.accountsCovered shouldBe 3
        }

    @Test
    fun `split by account, income counts for the account it came into`() = runBlocking<Unit> {
        val output = byAccount(TransactionType.INCOME)

        output.accountAmounts.map { it.account to it.amount } shouldBe listOf(Bank to 500.0)
    }

    // The month: two expenses from the bank, one from the card, a transfer between them, a salary
    // into the bank, and nothing in cash.
    private suspend fun byAccount(type: TransactionType) = PieChartUseCase(
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns listOf(Bank, Card, Cash) },
        transactionsInRangeUseCase = trnsInRange,
        // As the real one: income and expenses of all the transactions it's given, whatever the accounts.
        transactionsIncomeExpenseUseCase = mockk<TransactionsIncomeExpenseUseCase> {
            coEvery { incomeExpense(any(), any(), any()) } coAnswers {
                val transactions = firstArg<List<Transaction>>()
                IncomeExpenseTransferPair(
                    income = transactions.filterIsInstance<Income>().sumOf { it.value.amount.value.toBigDecimal() },
                    expense = transactions.filterIsInstance<Expense>().sumOf { it.value.amount.value.toBigDecimal() },
                    transferIncome = BigDecimal.ZERO,
                    transferExpense = BigDecimal.ZERO,
                )
            }
        },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        categoryIncomeExpenseUseCase = mockk<CategoryIncomeExpenseUseCase> {
            coEvery { incomeExpense(any(), any(), any(), any()) } returns IncomeExpenseTransferPair.zero()
        },
        resourceProvider = mockk<ResourceProvider> { every { getString(any()) } returns "Account transfers" },
        dispatchers = TestDispatchersProvider,
    ).pieChart(
        baseCurrency = "EUR",
        range = FromToTimeRange(from = Instant.EPOCH, to = Instant.EPOCH),
        type = type,
        accountIdFilterList = emptyList(),
        existingTransactions = listOf(
            previewTransaction(TransactionType.EXPENSE, Bank, 30.0),
            previewTransaction(TransactionType.EXPENSE, Bank, 40.0),
            previewTransaction(TransactionType.EXPENSE, Card, 20.0),
            previewTransaction(TransactionType.TRANSFER, Bank, 100.0, toAccount = Card),
            previewTransaction(TransactionType.INCOME, Bank, 500.0),
        ),
    )

    private suspend fun pieChart(onlyExisting: Boolean) = useCase.pieChart(
        baseCurrency = "EUR",
        range = FromToTimeRange(from = Instant.EPOCH, to = Instant.EPOCH),
        type = TransactionType.EXPENSE,
        accountIdFilterList = emptyList(),
        treatTransferAsIncExp = false,
        existingTransactions = emptyList(),
        onlyExisting = onlyExisting,
    )

    private companion object {
        val Bank = previewAccount("Bank", color = Palette.Blue, currency = "EUR", id = UUID(1L, 1L))
        val Card = previewAccount("Card", color = Palette.Purple1, currency = "EUR", id = UUID(1L, 2L))
        val Cash = previewAccount("Cash", color = Palette.Green, currency = "EUR", id = UUID(1L, 3L))
    }
}
