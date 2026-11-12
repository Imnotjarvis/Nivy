package com.nivy.reports.trends

import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.ConsumerPricesRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.reports.usecase.TrendsUseCase
import com.nivy.ui.testing.runTest
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class TrendsViewModelTest {
    // The ranges asked for, in order.
    private val asked = mutableListOf<TrendsRange>()

    private var base = Eur
    private var pricesDownloaded = false
    private val consumerPrices = mockk<ConsumerPricesRepository> { coEvery { syncIfDue() } returns false }

    private val viewModel = TrendsViewModel(
        trendsUseCase = mockk<TrendsUseCase> {
            coEvery { trends(any(), any(), any()) } answers {
                val range = firstArg<TrendsRange>()
                asked += range
                // Marked, when computed with the prices downloaded.
                val marked = persistentListOf(1.0).takeIf { pricesDownloaded }
                detail(months = range.months ?: 1).copy(realNetWorth = marked)
            }
        },
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } answers { base } },
        appPreferences = mockk<AppPreferences> { coEvery { startDayOfMonth() } returns 1 },
        consumerPricesRepository = consumerPrices,
    )

    @Test
    fun `a year of trends at first, the current month marked`() {
        viewModel.runTest {
            range shouldBe TrendsRange.Year
            detail?.months?.size shouldBe 12
            selectedIndex shouldBe 11
            baseCurrency shouldBe "EUR"
        }
    }

    @Test
    fun `another range loads its months, and marks the current one again`() {
        viewModel.runTest(
            events = listOf(TrendsEvent.SelectMonth(3), TrendsEvent.SelectRange(TrendsRange.ThreeMonths)),
        ) {
            range shouldBe TrendsRange.ThreeMonths
            detail?.months?.size shouldBe 3
            selectedIndex shouldBe 2
            asked.last() shouldBe TrendsRange.ThreeMonths
        }
    }

    @Test
    fun `a month picked on a chart is marked on all of them`() {
        viewModel.runTest(events = listOf(TrendsEvent.SelectMonth(4))) {
            selectedIndex shouldBe 4
        }
    }

    @Test
    fun `newer prices, once downloaded, load the trends again`() {
        // given
        coEvery { consumerPrices.syncIfDue() } answers {
            pricesDownloaded = true
            true
        }

        // then
        viewModel.runTest {
            detail?.realNetWorth shouldBe persistentListOf(1.0)
        }
    }

    @Test
    fun `prices are downloaded for euros only`() {
        // given
        base = AssetCode.USD

        // then
        viewModel.runTest {
            baseCurrency shouldBe "USD"
        }
        coVerify(exactly = 0) { consumerPrices.syncIfDue() }
    }

    private fun detail(months: Int) = TrendsDetail(
        months = (1..months).map { month ->
            MonthTrend(
                period = TimePeriod(month = Month.fromMonthValue(month), year = 2026),
                complete = month < months,
                full = month < months,
                income = 0.0,
                expense = 0.0,
                netWorth = 0.0,
                fixed = 0.0,
                habits = MonthHabits(0, 0.0, null, 0, 0, 0.0, 0.0, 0.0, 0.0),
            )
        }.toImmutableList(),
        accounts = persistentListOf(),
        categories = persistentListOf(),
        changes = persistentListOf(),
        habits = RangeHabits(null, null, null, null, 0.0, 15.0),
        punchCard = null,
        calendar = null,
        records = Records(null, null, null, null, null, null),
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
    }
}
