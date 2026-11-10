package com.nivy.reports.trends

import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.CurrencyRepository
import com.nivy.ui.testing.runTest
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class TrendsViewModelTest {
    // The ranges asked for, in order.
    private val asked = mutableListOf<TrendsRange>()

    private val viewModel = TrendsViewModel(
        trendsUseCase = mockk<TrendsUseCase> {
            coEvery { trends(any(), any(), any()) } answers {
                val range = firstArg<TrendsRange>()
                asked += range
                detail(months = range.months ?: 1)
            }
        },
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        appPreferences = mockk<AppPreferences> { coEvery { startDayOfMonth() } returns 1 },
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
