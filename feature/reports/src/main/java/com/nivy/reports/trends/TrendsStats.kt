package com.nivy.reports.trends

import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.forecast.line
import com.nivy.wallet.domain.pure.spending.BiggestPurchase
import com.nivy.wallet.domain.pure.spending.DayAmount
import com.nivy.wallet.domain.pure.spending.DayParts
import com.nivy.wallet.domain.pure.spending.EveningPart
import com.nivy.wallet.domain.pure.spending.Flow
import com.nivy.wallet.domain.pure.spending.Weekend
import com.nivy.wallet.domain.pure.spending.median
import com.nivy.wallet.domain.pure.spending.percentile
import kotlinx.collections.immutable.toImmutableList
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** A month of the trends: its period, its days, and whether it's over. */
internal data class MonthSpan(val period: TimePeriod, val days: List<LocalDate>, val complete: Boolean)

/** The net worth months ahead of the current one that the trends forecast. */
internal const val ForecastMonths = 3

// At most this many accounts and categories are drawn on their own, the others together.
private const val ListedAccounts = 3
private const val ListedCategories = 4
private const val ListedChanges = 5

// The categories' change compares the last of these months with as many before them.
private const val ChangeMonths = 3

// A milestone further than this many months away isn't told.
private const val MilestoneMonthsAtMost = 120

// The weeks of the spending calendar, at most.
private const val CalendarWeeks = 26

// The quartiles the spending calendar's darker days start from.
private val CalendarLevels = listOf(0.25, 0.5, 0.75)

/**
 * The trends of the range's [months] from the [flows] in them. [netWorth] and [balances] are
 * every account's at each month's end. Days before [trackedSince], before the app was used, aren't
 * days without spending. [changeMonths] are the complete months the categories' change compares.
 */
internal fun trends(
    months: List<MonthSpan>,
    changeMonths: List<MonthSpan>,
    flows: List<Flow>,
    netWorth: List<Double>,
    balances: List<Pair<Account, List<Double>>>,
    today: LocalDate,
    trackedSince: LocalDate,
    smallEdge: Double,
): TrendsDetail {
    val rangeDays = months.flatMap { it.days }.toSet()
    val inRange = flows.filter { it.day in rangeDays }
    val tracked = months.flatMap { it.days }.filter { !it.isBefore(trackedSince) && !it.isAfter(today) }
    val trend = months.mapIndexed { index, month ->
        monthTrend(month, inRange, netWorth[index], smallEdge, today, trackedSince)
    }
    val pace = if (netWorth.size > 1) line(netWorth).first else 0.0
    val current = months.lastOrNull()?.period?.let { YearMonth.of(it.year ?: today.year, it.month?.monthValue ?: 1) }
    return TrendsDetail(
        months = trend.toImmutableList(),
        forecast = forecast(netWorth).toImmutableList(),
        milestone = if (current != null) milestone(netWorth.lastOrNull() ?: 0.0, pace, current) else null,
        accounts = accountSeries(balances).toImmutableList(),
        categories = categorySeries(months, inRange).toImmutableList(),
        changes = categoryShifts(changeMonths.filter { !it.days.first().isBefore(trackedSince) }, flows)
            .toImmutableList(),
        habits = rangeHabits(inRange, tracked, smallEdge),
        punchCard = punchCard(tracked, inRange)?.map { it.toImmutableList() }?.toImmutableList(),
        calendar = spendingCalendar(tracked, inRange, today),
        records = records(trend, tracked, inRange),
    )
}

/** A month's sums and habits; its days so far from [trackedSince]. */
internal fun monthTrend(
    month: MonthSpan,
    flows: List<Flow>,
    netWorth: Double,
    smallEdge: Double,
    today: LocalDate,
    trackedSince: LocalDate,
): MonthTrend {
    val days = month.days.toSet()
    val inMonth = flows.filter { it.day in days }
    val expenses = inMonth.filter { !it.income }
    val purchases = expenses.filter { it.habit }
    val timed = purchases.filter { it.timed }
    val tracked = month.days.filter { !it.isBefore(trackedSince) && !it.isAfter(today) }
    val spendDays = purchases.map { it.day }.toSet()
    return MonthTrend(
        period = month.period,
        complete = month.complete,
        income = inMonth.filter { it.income }.sumOf { it.amount },
        expense = expenses.sumOf { it.amount },
        netWorth = netWorth,
        fixed = expenses.filter { it.recurring }.sumOf { it.amount },
        habits = MonthHabits(
            purchases = purchases.size,
            spent = purchases.sumOf { it.amount },
            typical = median(purchases.map { it.amount }),
            noSpendDays = tracked.count { it !in spendDays },
            days = tracked.size,
            weekend = purchases.filter { it.made.dayOfWeek in Weekend }.sumOf { it.amount },
            small = purchases.filter { it.amount < smallEdge }.sumOf { it.amount },
            evening = timed.filter { it.made.hour in DayParts[EveningPart] }.sumOf { it.amount },
            timed = timed.sumOf { it.amount },
        ),
        full = month.complete && !month.days.first().isBefore(trackedSince),
    )
}

/** The net worth for the next months, along the line that best fits [netWorth]'s months. */
internal fun forecast(netWorth: List<Double>, ahead: Int = ForecastMonths): List<Double> {
    if (netWorth.size < 2) return emptyList()
    val (slope, _) = line(netWorth)
    return (1..ahead).map { netWorth.last() + slope * it }
}

/**
 * The next round net worth above [netWorth] (a step of half its power of ten: 15,000 above
 * 12,147), and the month it's reached at [pace] a month from [now]; null when it's not reached.
 */
internal fun milestone(netWorth: Double, pace: Double, now: YearMonth): Milestone? {
    if (pace <= 0 || netWorth < 1) return null
    val step = 10.0.pow(floor(log10(netWorth))) / 2
    val next = (floor(netWorth / step) + 1) * step
    val months = ceil((next - netWorth) / pace).toLong()
    return if (months <= MilestoneMonthsAtMost) Milestone(next, now.plusMonths(months)) else null
}

/**
 * How much [values] changed from the first to the last, along the line that best fits them, of
 * where that line starts; null for fewer than two values, or a line that starts at nothing.
 */
internal fun trendChange(values: List<Double>): Double? {
    if (values.size < 2) return null
    val (slope, start) = line(values)
    if (start <= 0) return null
    return slope * (values.size - 1) / start
}

/** Each account's balance at each month's end, debts as nothing: the largest now, then the rest. */
internal fun accountSeries(balances: List<Pair<Account, List<Double>>>): List<AccountSeries> {
    val held = balances.map { (account, values) -> account to values.map { it.coerceAtLeast(0.0) } }
        .filter { (_, values) -> values.any { it > 0 } }
        .sortedByDescending { (_, values) -> values.last() }
    val listed = held.take(ListedAccounts).map { (account, values) -> AccountSeries(account, values.toImmutableList()) }
    val others = held.drop(ListedAccounts)
    if (others.isEmpty()) return listed
    val months = others.first().second.indices
    return listed + AccountSeries(null, months.map { month -> others.sumOf { it.second[month] } }.toImmutableList())
}

/** What each category took each month: the largest over the range first, then the others together. */
internal fun categorySeries(months: List<MonthSpan>, flows: List<Flow>): List<CategorySeries> {
    val purchases = flows.filter { it.purchase }
    val byMonth = months.map { month -> month.days.toSet().let { days -> purchases.filter { it.day in days } } }
    val ranked = purchases.groupBy { it.category }
        .mapValues { (_, inCategory) -> inCategory.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
        .map { it.key }
    fun series(categories: Set<Category?>) = byMonth.map { inMonth ->
        inMonth.filter { it.category in categories }.sumOf { it.amount }
    }.toImmutableList()
    val listed = ranked.take(ListedCategories).map { CategorySeries(it, other = false, series(setOf(it))) }
    val others = ranked.drop(ListedCategories).toSet()
    return if (others.isEmpty()) listed else listed + CategorySeries(null, other = true, series(others))
}

/**
 * The categories that changed the most: each one's monthly average over the last 3 of [months]
 * against the 3 before, the largest moves in amount first.
 */
internal fun categoryShifts(months: List<MonthSpan>, flows: List<Flow>): List<CategoryShift> {
    if (months.size < 2 * ChangeMonths) return emptyList()
    val before = months.takeLast(2 * ChangeMonths).take(ChangeMonths).flatMap { it.days }.toSet()
    val after = months.takeLast(ChangeMonths).flatMap { it.days }.toSet()
    val purchases = flows.filter { it.purchase && (it.day in before || it.day in after) }
    return purchases.groupBy { it.category }
        .map { (category, inCategory) ->
            val was = inCategory.filter { it.day in before }.sumOf { it.amount } / ChangeMonths
            val now = inCategory.filter { it.day in after }.sumOf { it.amount } / ChangeMonths
            Triple(category, was, now)
        }
        .sortedByDescending { (_, was, now) -> abs(now - was) }
        .take(ListedChanges)
        .map { (category, was, now) -> CategoryShift(category, now, was, if (was > 0) (now - was) / was else null) }
}

/** The habits of the range's [flows] over its [days] so far: the purchases made by choice. */
internal fun rangeHabits(flows: List<Flow>, days: List<LocalDate>, smallEdge: Double): RangeHabits {
    val purchases = flows.filter { it.habit }
    val spent = purchases.sumOf { it.amount }
    val timed = purchases.filter { it.timed }
    val timedSpent = timed.sumOf { it.amount }
    fun share(amount: Double, of: Double) = if (of > 0) amount / of else null
    return RangeHabits(
        typical = median(purchases.map { it.amount }),
        weekendShare = share(purchases.filter { it.made.dayOfWeek in Weekend }.sumOf { it.amount }, spent),
        smallShare = share(purchases.filter { it.amount < smallEdge }.sumOf { it.amount }, spent),
        eveningShare = share(timed.filter { it.made.hour in DayParts[EveningPart] }.sumOf { it.amount }, timedSpent),
        weekendDays = if (days.isEmpty()) 0.0 else days.count { it.dayOfWeek in Weekend }.toDouble() / days.size,
        smallUnder = smallEdge,
    )
}

/**
 * What's spent on average on each weekday of [days], Monday first, in each part of the day: the
 * purchases with a time of their own; null without any.
 */
internal fun punchCard(days: List<LocalDate>, flows: List<Flow>): List<List<Double>>? {
    val timed = flows.filter { it.habit && it.timed }
    if (timed.isEmpty()) return null
    return DayOfWeek.entries.map { weekday ->
        val times = days.count { it.dayOfWeek == weekday }
        DayParts.map { hours ->
            val spent = timed.filter { it.made.dayOfWeek == weekday && it.made.hour in hours }.sumOf { it.amount }
            if (times > 0) spent / times else 0.0
        }
    }
}

/** The last [weeks] weeks to today's of [days], what was spent on each; null without days. */
internal fun spendingCalendar(
    days: List<LocalDate>,
    flows: List<Flow>,
    today: LocalDate,
    weeks: Int = CalendarWeeks,
): SpendingCalendar? {
    if (days.isEmpty()) return null
    val counted = days.toSet()
    val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val start = maxOf(
        thisWeek.minusWeeks(weeks - 1L),
        days.first().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    )
    val spentOn = flows.filter { it.habit }.groupBy { it.day }.mapValues { (_, onDay) -> onDay.sumOf { it.amount } }
    val end = thisWeek.plusDays(DaysInWeek - 1L)
    val calendar = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    val amounts = calendar.map { day -> if (day in counted) spentOn[day] ?: 0.0 else null }
    val spending = amounts.filterNotNull().filter { it > 0 }.sorted()
    val top = calendar.zip(amounts).filter { (_, amount) -> amount != null && amount > 0 }
        .maxByOrNull { (_, amount) -> amount ?: 0.0 }
        ?.let { (day, amount) -> DayAmount(day, amount ?: 0.0) }
    val levels = if (spending.isEmpty()) emptyList() else CalendarLevels.map { percentile(spending, it) }
    return SpendingCalendar(
        start = start,
        amounts = amounts.toImmutableList(),
        levels = levels.toImmutableList(),
        top = top,
    )
}

/** A calendar day's shade, from 0 without spending to 4 at the top quarter of the days. */
internal fun calendarLevel(amount: Double, levels: List<Double>): Int =
    if (amount <= 0) 0 else 1 + levels.count { amount >= it }

/** The records of the full [months], and of the purchases made by choice over the [days] counted. */
internal fun records(months: List<MonthTrend>, days: List<LocalDate>, flows: List<Flow>): Records {
    val full = months.filter { it.full }
    val purchases = flows.filter { it.habit }
    val byDay = purchases.groupBy { it.day }
    val mostSaved = full.maxByOrNull { it.saved }?.takeIf { it.saved > 0 }
    val mostSpent = full.maxByOrNull { it.expense }?.takeIf { it.expense > 0 }
    val biggest = purchases.maxByOrNull { it.amount }
    return Records(
        mostSaved = mostSaved?.let { MonthAmount(it.period, it.saved) },
        mostSpent = mostSpent?.let { MonthAmount(it.period, it.expense) },
        biggest = biggest?.let { BiggestPurchase(it.amount, it.title, it.category, it.day) },
        noSpendStreak = longestWithout(days, byDay.keys),
        cheapestWeek = cheapestWeek(days, byDay),
        busiestDay = byDay.entries
            .maxWithOrNull(compareBy({ it.value.size }, { it.value.sumOf { flow -> flow.amount } }))
            ?.let { DayCount(it.key, it.value.size) },
    )
}

// The longest run of [days] none of [spendDays] falls on; null when every day had spending.
private fun longestWithout(days: List<LocalDate>, spendDays: Set<LocalDate>): DaySpan? {
    var longest: DaySpan? = null
    var from: LocalDate? = null
    for (day in days) {
        if (day in spendDays) {
            from = null
        } else {
            val start = from ?: day
            from = start
            val run = DaySpan(start, day)
            if (longest == null || run.days > longest.days) longest = run
        }
    }
    return longest
}

// Of the weeks, Monday to Sunday, whose days are all among [days], the one that cost the least.
private fun cheapestWeek(days: List<LocalDate>, byDay: Map<LocalDate, List<Flow>>): WeekAmount? {
    val counted = days.toSet()
    return days.filter { it.dayOfWeek == DayOfWeek.MONDAY && it.plusDays(DaysInWeek - 1L) in counted }
        .map { monday ->
            WeekAmount(
                monday,
                (0L until DaysInWeek).sumOf { byDay[monday.plusDays(it)].orEmpty().sumOf { flow -> flow.amount } }
            )
        }
        .minByOrNull { it.amount }
}
