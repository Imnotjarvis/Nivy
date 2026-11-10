package com.nivy.reports.trends

import com.nivy.base.model.AccountType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.spending.DayAmount
import com.nivy.wallet.domain.pure.spending.EveningPart
import com.nivy.wallet.domain.pure.spending.Flow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TrendsStatsTest {

    @Test
    fun `a month's income, expenses and fixed costs, and its habits without bills or loans`() {
        val trend =
            monthTrend(
                October,
                OctoberFlows,
                netWorth = 5_000.0,
                smallEdge = 15.0,
                today = Today,
                trackedSince = LongAgo
            )

        trend.income shouldBe 1_650.0
        trend.expense shouldBe (874.0 plusOrMinus Cent)
        trend.fixed shouldBe 550.0
        trend.saved shouldBe (776.0 plusOrMinus Cent)
        trend.savingsRate!! shouldBe (776.0 / 1_650.0 plusOrMinus Cent)
        trend.full shouldBe true
        // The rent, a planned payment, and the loan aren't habits.
        with(trend.habits) {
            purchases shouldBe 2
            typical shouldBe 12.0
            days shouldBe 31
            noSpendDays shouldBe 29
            // The shopping on Sunday the 4th.
            weekend shouldBe 20.0
            small shouldBe 4.0
            evening shouldBe 4.0
            timed shouldBe 24.0
        }
    }

    @Test
    fun `fixed and flexible are purchases only, and a planned loan repayment is neither`() {
        val repayment = spend(Oct(15), 200.0, recurring = true, purchase = false)

        val trend = monthTrend(October, OctoberFlows + repayment, 5_000.0, 15.0, Today, LongAgo)

        trend.fixed shouldBe 550.0
        // The groceries and the coffee.
        trend.flexible shouldBe 24.0
    }

    @Test
    fun `days before the app was used aren't days without spending, nor is the month full`() {
        val trend = monthTrend(October, OctoberFlows, 0.0, 15.0, Today, trackedSince = LocalDate.of(2026, 10, 5))

        trend.habits.days shouldBe 27
        // The 6th had spending.
        trend.habits.noSpendDays shouldBe 26
        trend.full shouldBe false
    }

    @Test
    fun `the current month counts its days up to today`() {
        val trend = monthTrend(November, emptyList(), 0.0, 15.0, Today, LongAgo)

        trend.habits.days shouldBe 9
        trend.complete shouldBe false
        trend.full shouldBe false
    }

    @Test
    fun `a change is told along the line that best fits, from where it starts`() {
        trendChange(listOf(10.0, 20.0, 30.0))!! shouldBe (2.0 plusOrMinus Cent)
        // A line from 47 rising 2 a step.
        trendChange(listOf(50.0, 40.0, 60.0, 50.0))!! shouldBe (6.0 / 47.0 plusOrMinus Cent)
        trendChange(listOf(0.0, 0.0)).shouldBeNull()
        trendChange(listOf(5.0)).shouldBeNull()
    }

    @Test
    fun `accounts are stacked largest first, debts as nothing, the rest together`() {
        val series = accountSeries(
            listOf(
                account("Cash") to listOf(100.0, 120.0),
                account("Bank") to listOf(2_000.0, 2_500.0),
                account("Card") to listOf(-300.0, -200.0),
                account("Savings") to listOf(1_000.0, 1_100.0),
                account("Wallet") to listOf(10.0, 15.0),
                account("Revolut") to listOf(50.0, 40.0),
            )
        )

        series.map { it.account?.name?.value } shouldBe listOf("Bank", "Savings", "Cash", null)
        series.last().values shouldBe listOf(60.0, 55.0)
    }

    @Test
    fun `categories are stacked largest first, the others together`() {
        val months = listOf(October, November)
        val flows = listOf(
            spend(Oct(10), 300.0, Housing),
            spend(Nov(2), 300.0, Housing),
            spend(Oct(11), 120.0, Groceries),
            spend(Nov(3), 90.0, Groceries),
            spend(Oct(12), 50.0, EatingOut),
            spend(Nov(4), 40.0, Transport),
            spend(Oct(13), 20.0, Leisure),
            spend(Nov(5), 5.0, null),
        )

        val series = categorySeries(months, flows)

        series.map { it.category } shouldBe listOf(Housing, Groceries, EatingOut, Transport, null)
        series.last().other shouldBe true
        series.last().values shouldBe listOf(20.0, 5.0)
        series.first().values shouldBe listOf(300.0, 300.0)
    }

    @Test
    fun `the biggest changes set the last 3 months against the 3 before, the largest moves first`() {
        val months = (5..10).map { month(it, complete = true) }
        val flows = listOf(
            // Groceries: 100 a month before, 120 after.
            spend(LocalDate.of(2026, 5, 10), 300.0, Groceries),
            spend(LocalDate.of(2026, 9, 10), 360.0, Groceries),
            // Eating out: new, 30 a month.
            spend(LocalDate.of(2026, 10, 10), 90.0, EatingOut),
            // Transport: 300 a month before, 150 after.
            spend(LocalDate.of(2026, 6, 10), 900.0, Transport),
            spend(LocalDate.of(2026, 8, 10), 450.0, Transport),
        )

        val shifts = categoryShifts(months, flows)

        shifts.map { it.category } shouldBe listOf(Transport, EatingOut, Groceries)
        shifts[0].change!! shouldBe (-0.5 plusOrMinus Cent)
        shifts[1].change.shouldBeNull()
        shifts[2].average shouldBe (120.0 plusOrMinus Cent)
        shifts[2].before shouldBe (100.0 plusOrMinus Cent)
        shifts[2].change!! shouldBe (0.2 plusOrMinus Cent)
        categoryShifts(months.drop(1), flows) shouldBe emptyList()
    }

    @Test
    fun `the range's habits, weekends, small purchases and evenings`() {
        val habits = rangeHabits(OctoberFlows, October.days, smallEdge = 15.0)

        habits.typical shouldBe 12.0
        habits.weekendShare!! shouldBe (20.0 / 24.0 plusOrMinus Cent)
        habits.smallShare!! shouldBe (4.0 / 24.0 plusOrMinus Cent)
        habits.eveningShare!! shouldBe (4.0 / 24.0 plusOrMinus Cent)
        // October 2026 has 9 weekend days of 31.
        habits.weekendDays shouldBe (9.0 / 31.0 plusOrMinus Cent)
    }

    @Test
    fun `the punch card averages each weekday's spending, at each part of the day`() {
        // Two Tuesdays in the days: the 6th and the 13th.
        val days = (5..18).map { Oct(it) }
        val flows = listOf(
            spend(Oct(6), 30.0, made = Oct(6).atTime(20, 0)),
            spend(Oct(13), 10.0, made = Oct(13).atTime(21, 0)),
            // Without a time of its own.
            spend(Oct(13), 99.0, made = Oct(13).atStartOfDay()),
        )

        val card = punchCard(days, flows)!!

        // Tuesday's evening.
        card[1][EveningPart] shouldBe 20.0
        card.sumOf { it.sum() } shouldBe 20.0
        punchCard(days, emptyList()).shouldBeNull()
    }

    @Test
    fun `the calendar runs from a Monday, its days shaded by quarters of the spending`() {
        val days = (14..31).map { Oct(it) } + (1..9).map { Nov(it) }
        val flows = listOf(10.0, 20.0, 30.0, 40.0, 50.0).mapIndexed { index, amount -> spend(Oct(20 + index), amount) }

        val calendar = spendingCalendar(days, flows, today = Today, weeks = 4)!!

        // Four weeks to today's: from Monday, October 19th, to Sunday, November 15th.
        calendar.start shouldBe Oct(19)
        calendar.amounts.size shouldBe 28
        calendar.amounts[1] shouldBe 10.0
        calendar.amounts[0] shouldBe 0.0
        // After today.
        calendar.amounts[22].shouldBeNull()
        calendar.levels shouldBe listOf(20.0, 30.0, 40.0)
        calendar.top shouldBe DayAmount(Oct(24), 50.0)
        calendarLevel(0.0, calendar.levels) shouldBe 0
        calendarLevel(10.0, calendar.levels) shouldBe 1
        calendarLevel(35.0, calendar.levels) shouldBe 3
        calendarLevel(50.0, calendar.levels) shouldBe 4
    }

    @Test
    fun `records are of the full months, and of the days counted`() {
        val months = listOf(
            monthTrend(
                month(9, complete = true),
                listOf(income(Sep(1), 1_000.0), spend(Sep(2), 100.0)),
                0.0,
                15.0,
                Today,
                LongAgo
            ),
            monthTrend(October, OctoberFlows, 0.0, 15.0, Today, LongAgo),
            // The current month isn't full: its saving isn't a record yet.
            monthTrend(November, listOf(income(Nov(2), 5_000.0)), 0.0, 15.0, Today, LongAgo),
        )
        val days = (1..30).map { Sep(it) }
        val flows = listOf(
            spend(Sep(2), 100.0, title = "Laptop"),
            spend(Sep(2), 5.0),
            spend(Sep(3), 8.0),
            // A week without spending, from Monday the 14th: the rent paid on the 15th is a bill.
            spend(Sep(11), 20.0),
            spend(Sep(15), 550.0, recurring = true),
            spend(Sep(21), 30.0),
        )

        val records = records(months, days, flows)

        records.mostSaved shouldBe MonthAmount(month(9, complete = true).period, 900.0)
        records.mostSpent!!.amount shouldBe (874.0 plusOrMinus Cent)
        records.biggest!!.title shouldBe "Laptop"
        records.noSpendStreak shouldBe DaySpan(Sep(12), Sep(20))
        records.noSpendStreak!!.days shouldBe 9
        records.cheapestWeek shouldBe WeekAmount(Sep(14), 0.0)
        records.busiestDay shouldBe DayCount(Sep(2), 2)
    }

    private companion object {
        const val Cent = 0.01

        /** Monday, November 9, 2026. */
        val Today: LocalDate = LocalDate.of(2026, 11, 9)
        val LongAgo: LocalDate = LocalDate.of(2020, 1, 1)

        val October = month(10, complete = true)
        val November = month(11, complete = false)

        val Housing = category("Housing", 1)
        val Groceries = category("Groceries", 2)
        val EatingOut = category("Eating out", 3)
        val Transport = category("Transport", 4)
        val Leisure = category("Leisure", 5)

        // October 2026: the salary, the rent on Saturday the 3rd, shopping on Sunday the 4th, a
        // coffee on the evening of Tuesday the 6th and a loan paid back.
        val OctoberFlows = listOf(
            income(Oct(1), 1_650.0),
            spend(Oct(3), 550.0, Housing, recurring = true),
            spend(Oct(4), 20.0, Groceries, made = Oct(4).atTime(10, 0)),
            spend(Oct(6), 4.0, EatingOut, made = Oct(6).atTime(20, 0)),
            spend(Oct(7), 300.0, purchase = false),
        )

        fun Oct(day: Int): LocalDate = LocalDate.of(2026, 10, day)
        fun Nov(day: Int): LocalDate = LocalDate.of(2026, 11, day)
        fun Sep(day: Int): LocalDate = LocalDate.of(2026, 9, day)

        fun month(month: Int, complete: Boolean): MonthSpan {
            val first = LocalDate.of(2026, month, 1)
            return MonthSpan(
                period = TimePeriod(month = Month.fromMonthValue(month), year = 2026),
                days = (0 until first.lengthOfMonth()).map { first.plusDays(it.toLong()) },
                complete = complete,
            )
        }

        fun spend(
            day: LocalDate,
            amount: Double,
            category: Category? = null,
            recurring: Boolean = false,
            made: LocalDateTime = day.atTime(9, 0),
            purchase: Boolean = true,
            title: String? = null,
        ) = Flow(
            amount = amount,
            income = false,
            day = day,
            made = made,
            title = title,
            category = category,
            account = null,
            recurring = recurring,
            purchase = purchase,
        )

        fun income(day: LocalDate, amount: Double) = Flow(
            amount = amount,
            income = true,
            day = day,
            made = day.atTime(9, 0),
            title = null,
            category = null,
            account = null,
            recurring = false,
            purchase = false,
        )

        fun category(name: String, n: Long) = Category(
            id = CategoryId(UUID(9L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(0),
            icon = null,
            orderNum = n.toDouble(),
        )

        fun account(name: String) = Account(
            id = AccountId(UUID.nameUUIDFromBytes(name.toByteArray())),
            name = NotBlankTrimmedString.unsafe(name),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
