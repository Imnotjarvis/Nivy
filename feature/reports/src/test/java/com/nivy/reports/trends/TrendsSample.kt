package com.nivy.reports.trends

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import kotlinx.collections.immutable.toImmutableList
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random

/**
 * Trends on Monday, November 9th, 2026, for the screenshots: a year of a made-up person's money,
 * the same every time, figured as the screen figures it.
 */
internal object TrendsSample {
    /** November 9, 2026, noon, in UTC. */
    val November9 = object : TimeProvider {
        private val now = LocalDateTime.of(2026, 11, 9, 12, 0)
        override fun getZoneId(): ZoneId = ZoneId.of("UTC")
        override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
        override fun localNow(): LocalDateTime = now
        override fun localDateNow(): LocalDate = now.toLocalDate()
        override fun localTimeNow(): LocalTime = now.toLocalTime()
    }

    private val Today = LocalDate.of(2026, 11, 9)
    private val Current = YearMonth.of(2026, 11)

    private val Housing = category("Housing", Palette.Blue3, "house", 1)
    private val Groceries = category("Groceries", Palette.Green2, "groceries", 2)
    private val EatingOut = category("Eating out", Palette.Orange, "restaurant", 3)
    private val Transport = category("Transport", Palette.Blue, "transport", 4)
    private val Subscriptions = category("Subscriptions", Palette.Red, "music", 5)
    private val Shopping = category("Shopping", Palette.Purple1, "shopping", 6)
    private val Leisure = category("Leisure", Palette.Yellow, "game", 7)
    private val Bills = category("Bills", Palette.Orange2, "bills", 8)

    private val MainBank = account("Main Bank", Palette.Blue3, 1)
    private val Savings = account("Savings", Palette.Green, 2)
    private val Revolut = account("Revolut", Palette.Purple1, 3)
    private val Cash = account("Cash", Palette.Orange3, 4)
    private val Investments = account("Investments", Palette.Green3, 5)
    private val CreditCard = account("Credit Card", Palette.Red, 6)

    private fun span(month: YearMonth) = MonthSpan(
        period = TimePeriod(month = Month.fromMonthValue(month.monthValue), year = month.year),
        days = (1..month.lengthOfMonth()).map { month.atDay(it) },
        complete = month.isBefore(Current),
    )

    private val Months = (11L downTo 0L).map { span(Current.minusMonths(it)) }
    private val ChangeMonths = (6L downTo 1L).map { span(Current.minusMonths(it)) }

    // A year's salaries, bills and purchases: lunches out from May, a gym from June, cheaper groceries
    // from July, and fewer rides month by month.
    private val Flows: List<Flow> = run {
        val random = Random(11)
        val first = Months.first().days.first()
        generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(Today) }.flatMap { day ->
            val month = Months.indexOfFirst { day in it.days }
            val weekend = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
            val cheaper = !day.isBefore(LocalDate.of(2026, 7, 1))
            buildList {
                if (day.dayOfMonth == 1) add(income(day, 1_650.0))
                if (day == LocalDate.of(2026, 3, 15)) add(income(day, 500.0))
                if (day == LocalDate.of(2026, 7, 10)) add(income(day, 330.0))
                if (day.dayOfMonth == 2) add(spend(day, 7, "Spotify", Subscriptions, 10.99, recurring = true))
                if (day.dayOfMonth == 3) add(spend(day, 9, "Rent", Housing, 550.0, recurring = true))
                if (day.dayOfMonth == 4) add(spend(day, 6, "Netflix", Subscriptions, 13.99, recurring = true))
                if (day.dayOfMonth == 6 && !day.isBefore(LocalDate.of(2026, 6, 1))) {
                    add(spend(day, 7, "Gym", Leisure, 34.90, recurring = true))
                }
                if (day.dayOfMonth == 15) {
                    add(
                        spend(day, 9, "Electricity", Bills, 40.0 + random.nextInt(30), recurring = true)
                    )
                }
                if (day == LocalDate.of(2026, 3, 14)) add(spend(day, 16, "Laptop", Shopping, 1_240.0))
                if (random.nextInt(3) == 0) {
                    val amount = if (cheaper) 6.0 + random.nextInt(14) else 12.0 + random.nextInt(38)
                    val title = if (random.nextBoolean()) "Lidl" else "Bakery"
                    add(spend(day, 18 + random.nextInt(2), title, Groceries, amount))
                }
                if (!weekend && random.nextInt(100) < 30) {
                    add(spend(day, 8, "Bar Roma", EatingOut, 2.5 + random.nextInt(3)))
                }
                if (!weekend && !day.isBefore(LocalDate.of(2026, 5, 1)) && random.nextInt(100) < 45) {
                    add(spend(day, 13, "Lunch place", EatingOut, 9.0 + random.nextInt(6)))
                }
                if (weekend && random.nextInt(100) < 30) {
                    add(spend(day, 20 + random.nextInt(2), "Trattoria", EatingOut, 25.0 + random.nextInt(20)))
                }
                if (random.nextInt(100) < 60 - 4 * month) {
                    add(spend(day, 7 + random.nextInt(16), "Bus", Transport, 6.0 + random.nextInt(6)))
                }
                if (random.nextInt(25) == 0) {
                    add(
                        spend(day, 14 + random.nextInt(7), "Home goods", Shopping, 30.0 + random.nextInt(120))
                    )
                }
                if (day.dayOfWeek == DayOfWeek.FRIDAY && random.nextInt(3) == 0) {
                    add(
                        spend(day, 21, "Cinema", Leisure, 12.0 + random.nextInt(12))
                    )
                }
            }
        }.toList()
    }

    // What the person held at each month's end: the bank takes what the other accounts don't.
    private val Balances: List<Pair<Account, List<Double>>> = run {
        val saved = Months.map { month -> month.days.toSet().let { days -> Flows.filter { it.day in days } } }
            .map { inMonth -> inMonth.sumOf { if (it.income) it.amount else -it.amount } }
        val netWorth = saved.runningFold(8_600.0) { worth, month -> worth + month + 35.0 }.drop(1)
        val savings = Months.indices.map { 2_800.0 + 150.0 * it }
        val revolut = Months.indices.map { 250.0 + 12.5 * it + (it % 3) * 20.0 }
        val cash = Months.indices.map { 340.0 + (it % 4) * 25.0 }
        val investments = Months.indices.map { 1_500.0 + 60.0 * it + if (it % 5 == 0) -80.0 else 0.0 }
        val card = Months.indices.map { -(380.0 + (it % 3) * 110.0) }
        val bank = Months.indices.map { month ->
            netWorth[month] - savings[month] - revolut[month] - cash[month] - investments[month] - card[month]
        }
        listOf(
            MainBank to bank,
            Savings to savings,
            Revolut to revolut,
            Cash to cash,
            Investments to investments,
            CreditCard to card,
        )
    }

    private val NetWorth = Months.indices.map { index -> Balances.sumOf { (_, values) -> values[index] } }

    val Detail: TrendsDetail = trends(
        months = Months,
        changeMonths = ChangeMonths,
        flows = Flows,
        netWorth = NetWorth,
        balances = Balances,
        today = Today,
        trackedSince = LocalDate.of(2024, 3, 1),
        smallEdge = 15.0,
    ).let { detail ->
        detail.copy(
            outlook = netWorthOutlook(
                start = NetWorth.last(),
                changes = (1 until Months.size).filter { Months[it].complete }.map { NetWorth[it] - NetWorth[it - 1] },
                now = Current,
                random = Random(Today.toEpochDay()),
            ),
            shifts = habitShifts(detail.months, Months, Flows, atLeast = 10.0).toImmutableList(),
        )
    }

    /** A year of trends, July marked, as a tap on its slot leaves it. */
    val State = TrendsState(baseCurrency = "EUR", range = TrendsRange.Year, detail = Detail, selectedIndex = 7)

    private fun spend(
        day: LocalDate,
        hour: Int,
        title: String,
        category: Category,
        amount: Double,
        recurring: Boolean = false,
    ) = Flow(
        amount = amount,
        income = false,
        day = day,
        made = day.atTime(hour, 0),
        title = title,
        category = category,
        recurring = recurring,
        purchase = true,
    )

    private fun income(day: LocalDate, amount: Double) = Flow(
        amount = amount,
        income = true,
        day = day,
        made = day.atTime(9, 0),
        title = "Salary",
        category = null,
        recurring = false,
        purchase = false,
    )

    private fun category(name: String, color: Color, icon: String, n: Long) = Category(
        id = CategoryId(UUID(9L, n)),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(color.toArgb()),
        icon = IconAsset.unsafe(icon),
        orderNum = n.toDouble(),
    )

    private fun account(name: String, color: Color, n: Long) = Account(
        id = AccountId(UUID(1L, n)),
        name = NotBlankTrimmedString.unsafe(name),
        asset = AssetCode.unsafe("EUR"),
        color = ColorInt(color.toArgb()),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = n.toDouble(),
    )
}
