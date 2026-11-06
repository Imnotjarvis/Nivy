package com.ivy.piechart

import com.ivy.base.model.TransactionType
import com.ivy.base.resource.ResourceProvider
import com.ivy.data.repository.CategoryRepository
import com.ivy.piechart.action.PieChartAct
import com.ivy.wallet.data.model.FromToTimeRange
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.category.CategoryIncomeWithAccountFiltersAct
import com.ivy.wallet.domain.action.transaction.CalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.transaction.TrnsWithRangeAndAccFiltersAct
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
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

class PieChartActTest {
    private val trnsInRange = mockk<TrnsWithRangeAndAccFiltersAct> {
        coEvery { this@mockk(any()) } returns emptyList()
    }
    private val act = PieChartAct(
        accountsAct = mockk<AccountsAct> { coEvery { this@mockk() } returns persistentListOf() },
        trnsWithRangeAndAccFiltersAct = trnsInRange,
        calcTrnsIncomeExpenseAct = mockk<CalcTrnsIncomeExpenseAct> {
            coEvery { this@mockk(any()) } returns IncomeExpenseTransferPair.zero()
        },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        categoryIncomeWithAccountFiltersAct = mockk<CategoryIncomeWithAccountFiltersAct> {
            coEvery { this@mockk(any()) } returns IncomeExpenseTransferPair.zero()
        },
        resourceProvider = mockk<ResourceProvider> { every { getString(any()) } returns "Account transfers" },
    )

    @Test
    fun `a budget's page charts only its transactions, even none`() = runBlocking {
        val output = act(input(onlyExisting = true))

        output.totalAmount shouldBe 0.0
        output.categoryAmounts.shouldBeEmpty()
        coVerify(exactly = 0) { trnsInRange(any()) }
    }

    @Test
    fun `without transactions given, the chart reads the month's`() = runBlocking {
        act(input(onlyExisting = false))

        coVerify(exactly = 1) { trnsInRange(any()) }
    }

    private fun input(onlyExisting: Boolean) = PieChartAct.Input(
        baseCurrency = "EUR",
        range = FromToTimeRange(from = Instant.EPOCH, to = Instant.EPOCH),
        type = TransactionType.EXPENSE,
        accountIdFilterList = emptyList(),
        treatTransferAsIncExp = false,
        existingTransactions = emptyList(),
        onlyExisting = onlyExisting,
    )
}
