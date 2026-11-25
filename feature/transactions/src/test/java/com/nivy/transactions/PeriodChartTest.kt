package com.nivy.transactions

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate

class PeriodChartTest {

    @Test
    fun `a month's lines add up each day to today, leaving out what's dated later`() {
        // given
        val flows = listOf(
            Flow(nov(1), expense = 550.0),
            Flow(nov(10), income = 1_850.0),
            Flow(nov(24), expense = 3.75),
            Flow(nov(28), expense = 100.0),
        )

        // when
        val chart = accountChart(November, today = nov(24), flows = flows)!!

        // then
        chart.step shouldBe ChartStep.Day
        chart.points.size shouldBe 30
        chart.through shouldBe nov(24)
        chart.income!!.size shouldBe 24
        chart.income[8] shouldBe 0.0
        chart.income[9] shouldBe 1_850.0
        chart.expenses!!.first() shouldBe 550.0
        chart.expenses.last() shouldBe 553.75
        chart.before shouldBe null
    }

    @Test
    fun `a past period's lines run to its last day`() {
        // given
        val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        val flows = listOf(Flow(LocalDate.of(2026, 10, 3), income = 5.0))

        // when
        val chart = accountChart(october, today = nov(24), flows = flows)!!

        // then
        chart.through shouldBe LocalDate.of(2026, 10, 31)
        chart.income!!.size shouldBe 31
        chart.income.last() shouldBe 5.0
    }

    @Test
    fun `a period that hasn't started, or where nothing moved yet, has no chart`() {
        val december = LocalDate.of(2026, 12, 1)..LocalDate.of(2026, 12, 31)

        val inDecember = listOf(Flow(LocalDate.of(2026, 12, 2), expense = 5.0))

        accountChart(december, today = nov(24), flows = inDecember) shouldBe null
        accountChart(November, today = nov(24), flows = emptyList()) shouldBe null
        accountChart(November, today = nov(24), flows = listOf(Flow(nov(28), expense = 5.0))) shouldBe null
        // A transfer, when transfers don't count, moves nothing.
        accountChart(November, today = nov(24), flows = listOf(Flow(nov(5)))) shouldBe null
    }

    @Test
    fun `the step follows the period's length, a day to 3 months, a month to 3 years, then a year`() {
        // The last 3 months, and the last 3 years, from November 25.
        chartStep(LocalDate.of(2026, 8, 25)..LocalDate.of(2026, 11, 25)) shouldBe ChartStep.Day
        chartStep(LocalDate.of(2026, 8, 25)..LocalDate.of(2026, 11, 26)) shouldBe ChartStep.Month
        chartStep(LocalDate.of(2023, 11, 25)..LocalDate.of(2026, 11, 25)) shouldBe ChartStep.Month
        chartStep(LocalDate.of(2023, 11, 25)..LocalDate.of(2026, 11, 26)) shouldBe ChartStep.Year
    }

    @Test
    fun `after the first, a longer period's points start each month, or each year`() {
        val days = LocalDate.of(2023, 3, 15)..LocalDate.of(2026, 6, 10)

        chartPoints(LocalDate.of(2026, 3, 15)..LocalDate.of(2026, 6, 10), ChartStep.Month) shouldContainExactly listOf(
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 6, 1),
        )
        chartPoints(days, ChartStep.Year) shouldContainExactly listOf(
            LocalDate.of(2023, 3, 15),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2026, 1, 1),
        )
    }

    @Test
    fun `a year adds up by month, to today's`() {
        // given
        val year = LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31)
        val flows = listOf(
            Flow(LocalDate.of(2026, 1, 10), income = 100.0),
            Flow(nov(20), expense = 50.0),
            Flow(nov(28), expense = 7.0),
        )

        // when
        val chart = accountChart(year, today = nov(24), flows = flows)!!

        // then
        chart.step shouldBe ChartStep.Month
        chart.points.size shouldBe 12
        chart.income shouldBe List(11) { 100.0 }
        chart.expenses!!.last() shouldBe 50.0
        chart.expenses[9] shouldBe 0.0
    }

    @Test
    fun `the period before a month is the month before, before whole months as many, else as many days`() {
        periodBefore(November) shouldBe LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        periodBefore(LocalDate.of(2026, 10, 25)..nov(24)) shouldBe
            LocalDate.of(2026, 9, 25)..LocalDate.of(2026, 10, 24)
        periodBefore(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31)) shouldBe
            LocalDate.of(2025, 1, 1)..LocalDate.of(2025, 12, 31)
        periodBefore(nov(3)..nov(12)) shouldBe LocalDate.of(2026, 10, 24)..nov(2)
    }

    @Test
    fun `a category's line is its spending, over the period before's, compared by the same day`() {
        // given
        val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        val now = listOf(Flow(nov(2), expense = 18.0), Flow(nov(22), expense = 47.5))
        val before = listOf(
            Flow(LocalDate.of(2026, 10, 3), expense = 25.0),
            Flow(LocalDate.of(2026, 10, 24), expense = 38.5),
            Flow(LocalDate.of(2026, 10, 31), expense = 41.75),
        )

        // when
        val chart = categoryChart(November, today = nov(24), flows = now, before = october to before)!!

        // then
        chart.income shouldBe null
        chart.expenses!!.size shouldBe 24
        chart.expenses.last() shouldBe 65.5
        chart.before!!.points.size shouldBe 31
        chart.before.addedUp.last() shouldBe 105.25
        chart.before.sameDay shouldBe LocalDate.of(2026, 10, 24)
        chart.before.bySameDay shouldBe 63.5
    }

    @Test
    fun `a category that brings in more than it takes out draws its income`() {
        val chart = categoryChart(
            November,
            today = nov(24),
            flows = listOf(Flow(nov(10), income = 1_850.0), Flow(nov(12), expense = 20.0)),
            before = null,
        )!!

        chart.expenses shouldBe null
        chart.income!!.last() shouldBe 1_850.0
    }

    @Test
    fun `a category with nothing the period before isn't compared, and with nothing yet still is`() {
        val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        val spent = listOf(Flow(LocalDate.of(2026, 10, 5), expense = 30.0))

        val spentNow = listOf(Flow(nov(2), expense = 18.0))

        categoryChart(November, nov(24), flows = spentNow, before = october to emptyList())!!.before shouldBe null
        val nothingYet = categoryChart(November, nov(24), flows = emptyList(), before = october to spent)!!
        nothingYet.expenses shouldBe List(24) { 0.0 }
        nothingYet.before!!.bySameDay shouldBe 30.0
        categoryChart(November, nov(24), flows = emptyList(), before = october to emptyList()) shouldBe null
    }

    private companion object {
        val November = nov(1)..nov(30)

        fun nov(day: Int): LocalDate = LocalDate.of(2026, 11, day)
    }
}
