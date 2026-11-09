package com.nivy.balance.detail

import com.nivy.base.model.AccountType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MonthStatsTest {

    @Test
    fun `a month's purchases tell how much, how often and how big`() {
        val habits = november()!!

        habits.spent shouldBe (352.85 plusOrMinus Cent)
        habits.perDay shouldBe (39.21 plusOrMinus Cent)
        habits.purchases shouldBe 17
        habits.median shouldBe (10.99 plusOrMinus Cent)
        habits.average shouldBe (20.76 plusOrMinus Cent)
        habits.biggest?.title shouldBe "Home goods"
        habits.savedShare!! shouldBe ((1_650.0 - 352.85) / 1_650.0 plusOrMinus Cent)
    }

    @Test
    fun `when it's spent, the busiest weekday, weekends against weekdays, and the time of day`() {
        val habits = november()!!

        habits.busiest shouldBe DayOfWeek.THURSDAY
        habits.byWeekday[DayOfWeek.THURSDAY.ordinal] shouldBe (130.30 plusOrMinus Cent)
        // Three weekend days at 154.30 in all, six weekdays at 198.55.
        habits.weekendRatio!! shouldBe (154.30 / 3 / (198.55 / 6) plusOrMinus Cent)
        habits.noSpendDays shouldBe 2
        // Morning, afternoon, evening, night.
        val amounts = habits.timeOfDay!!.map { it * 352.85 }
        amounts[0] shouldBe (66.98 plusOrMinus Cent)
        amounts[1] shouldBe (136.66 plusOrMinus Cent)
        amounts[2] shouldBe (136.31 plusOrMinus Cent)
        amounts[Night] shouldBe (12.90 plusOrMinus Cent)
    }

    @Test
    fun `a purchase at midnight sharp has no time of its own, and stays out of the time of day`() {
        val untimed = Purchases + spend(9, 0, "Rent top-up", Housing, 500.0)

        val habits = habits(untimed)!!

        habits.timeOfDay!!.sum() shouldBe (1.0 plusOrMinus Cent)
        habits.timeOfDay[Night] shouldBe (12.90 / 352.85 plusOrMinus Cent)
    }

    @Test
    fun `sizes and small purchases, in bands from the edges`() {
        val habits = november()!!

        habits.sizes.map { it.count } shouldBe listOf(4, 7, 4, 2, 0)
        habits.small.count shouldBe 11
        habits.small.share shouldBe (78.24 / 352.85 plusOrMinus Cent)
    }

    @Test
    fun `fixed is the bills and subscriptions paid, and those still due`() {
        val habits = november()!!

        habits.fixed.amount shouldBe (24.98 + 745.55 plusOrMinus Cent)
        habits.fixed.count shouldBe 6
        habits.flexible.amount shouldBe (352.85 - 24.98 plusOrMinus Cent)
        habits.flexible.count shouldBe 15
    }

    @Test
    fun `categories against as many days of the month before, new where there was none`() {
        val habits = november()!!

        val changes = habits.categories.associateBy { it.category }
        changes.getValue(Shopping).change.shouldBeNull()
        changes.getValue(Groceries).change!! shouldBe ((64.16 - 72.90) / 72.90 plusOrMinus Cent)
        habits.categories.first().category shouldBe Shopping
    }

    @Test
    fun `places gone back to, whatever the case of their titles, the most visited first`() {
        val habits = habits(Purchases + spend(9, 21, "bar roma", EatingOut, 4.0))!!

        habits.topPlace!!.name shouldBe "Bar Roma"
        habits.topPlace.visits shouldBe 6
        habits.places.map { it.name } shouldBe listOf("Bar Roma", "Lidl", "Uber", "Bakery")
    }

    @Test
    fun `no purchases, no habits`() {
        habits(emptyList()).shouldBeNull()
    }

    @Test
    fun `free to spend is what's left a day for the month to end even`() {
        val free = freeToSpend(
            income = 1_650.0,
            plannedIncome = 0.0,
            spent = 352.85,
            plannedExpenses = 745.55,
            daysLeft = 21
        )!!

        free.left shouldBe (551.60 plusOrMinus Cent)
        free.perDay shouldBe (551.60 / 21 plusOrMinus Cent)
    }

    @Test
    fun `once more went out than came in, nothing is left to spend`() {
        val free = freeToSpend(
            income = 100.0,
            plannedIncome = 0.0,
            spent = 150.0,
            plannedExpenses = 0.0,
            daysLeft = 10
        )!!

        free.left shouldBe (-50.0 plusOrMinus Cent)
    }

    @Test
    fun `without income this month or a day left, there's no free to spend`() {
        freeToSpend(
            income = 0.0,
            plannedIncome = 0.0,
            spent = 10.0,
            plannedExpenses = 0.0,
            daysLeft = 10
        ).shouldBeNull()
        freeToSpend(
            income = 100.0,
            plannedIncome = 0.0,
            spent = 10.0,
            plannedExpenses = 0.0,
            daysLeft = 0
        ).shouldBeNull()
    }

    @Test
    fun `the lowest point ahead, when below today's balance`() {
        val days = (1..5).map { LocalDate.of(2026, 11, it) }

        lowestAhead(days, listOf(100.0, 90.0, 85.0, 40.0, 60.0), actualDays = 2) shouldBe DayAmount(days[3], 40.0)
        lowestAhead(days, listOf(100.0, 90.0, 95.0, 100.0, 120.0), actualDays = 2).shouldBeNull()
    }

    @Test
    fun `the money lasts the net worth over the months that had spending`() {
        val runway = runway(netWorth = 12_000.0, monthlySpending = listOf(1_000.0, 0.0, 1_400.0))!!

        runway.monthly shouldBe 1_200.0
        runway.months shouldBe 10.0
        runway.over shouldBe 2
        runway(netWorth = -5.0, monthlySpending = listOf(1_000.0)).shouldBeNull()
        runway(netWorth = 500.0, monthlySpending = listOf(0.0)).shouldBeNull()
    }

    @Test
    fun `size bands are round amounts in other currencies`() {
        sizeEdges(1.0) shouldBe listOf(5.0, 15.0, 50.0, 150.0)
        // A euro is about 163 yen.
        sizeEdges(163.4) shouldBe listOf(1_000.0, 2_500.0, 10_000.0, 25_000.0)
    }

    private fun november(): Habits? = habits(Purchases)

    private fun habits(spends: List<Spend>): Habits? = habits(
        spends = spends,
        elapsed = (1..9).map { LocalDate.of(2026, 11, it) },
        daysInMonth = 30,
        income = 1_650.0,
        outgoing = spends.sumOf { it.amount },
        plannedExpenses = listOf(35.0, 120.55, 40.0, 550.0),
        previous = mapOf(Groceries to 72.90, EatingOut to 34.80, Transport to 46.20, Subscriptions to 24.98),
        edges = SizeEdgesInEuros,
    )

    private companion object {
        const val Cent = 0.01
        const val Night = 3

        val Groceries = category("Groceries", 1)
        val EatingOut = category("Eating out", 2)
        val Transport = category("Transport", 3)
        val Shopping = category("Shopping", 4)
        val Subscriptions = category("Subscriptions", 5)
        val Leisure = category("Leisure", 6)
        val Housing = category("Housing", 7)
        val Bank = Account(
            id = AccountId(UUID(1L, 1L)),
            name = NotBlankTrimmedString.unsafe("Main Bank"),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )

        // November 2026's purchases by the 9th, a Monday: November 1st is a Sunday.
        val Purchases = listOf(
            spend(1, 9, "Bar Roma", EatingOut, 3.80),
            spend(1, 18, "Lidl", Groceries, 18.50),
            spend(2, 7, "Spotify", Subscriptions, 10.99, recurring = true),
            spend(2, 8, "Bar Roma", EatingOut, 2.50),
            spend(2, 23, "Uber", Transport, 18.81),
            spend(4, 6, "Netflix", Subscriptions, 13.99, recurring = true),
            spend(4, 9, "Bar Roma", EatingOut, 3.20),
            spend(4, 12, "Bakery", Groceries, 6.36),
            spend(5, 17, "Home goods", Shopping, 130.30),
            spend(7, 11, "Lidl", Groceries, 24.60),
            spend(7, 20, "Trattoria", EatingOut, 58.40),
            spend(7, 22, "Bar Roma", EatingOut, 7.00),
            spend(8, 1, "Uber", Transport, 12.90),
            spend(8, 10, "Bakery", Groceries, 5.10),
            spend(8, 21, "Cinema", Leisure, 24.00),
            spend(9, 8, "Bar Roma", EatingOut, 2.80),
            spend(9, 19, "Lidl", Groceries, 9.60),
        )

        fun spend(day: Int, hour: Int, title: String, category: Category, amount: Double, recurring: Boolean = false) =
            Spend(
                amount = amount,
                day = LocalDate.of(2026, 11, day),
                made = LocalDateTime.of(2026, 11, day, hour, 0),
                title = title,
                category = category,
                account = Bank,
                recurring = recurring,
            )

        fun category(name: String, n: Long) = Category(
            id = CategoryId(UUID(9L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(0),
            icon = null,
            orderNum = n.toDouble(),
        )
    }
}
