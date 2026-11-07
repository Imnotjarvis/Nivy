package com.nivy.piechart

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.TransactionType
import com.nivy.base.resource.ResourceProvider
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.domain.usecase.category.CategoryIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsInRangeUseCase
import com.nivy.piechart.usecase.PieChartUseCase
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

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

    private suspend fun pieChart(onlyExisting: Boolean) = useCase.pieChart(
        baseCurrency = "EUR",
        range = FromToTimeRange(from = Instant.EPOCH, to = Instant.EPOCH),
        type = TransactionType.EXPENSE,
        accountIdFilterList = emptyList(),
        treatTransferAsIncExp = false,
        existingTransactions = emptyList(),
        onlyExisting = onlyExisting,
    )
}
