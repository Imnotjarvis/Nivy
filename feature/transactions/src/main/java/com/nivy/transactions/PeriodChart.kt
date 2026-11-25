package com.nivy.transactions

import androidx.compose.runtime.Immutable
import com.nivy.base.time.TimeConverter
import com.nivy.wallet.data.model.FromToTimeRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How far apart a period chart's points are: a day, a month or a year, as long as the period is. */
enum class ChartStep { Day, Month, Year }

/** What came in and went out on a [day], in the page's currency. */
internal data class Flow(val day: LocalDate, val income: Double = 0.0, val expense: Double = 0.0) {
    val moved: Boolean get() = income != 0.0 || expense != 0.0
}

/**
 * A period's money added up a [step] at a time, a point at each step's end: [income] and [expenses]
 * up to the point the period got to, null where the page doesn't draw them. On a category's page,
 * [before] is the period before's, to compare with.
 */
@Immutable
data class PeriodChart(
    val step: ChartStep,
    /** Each point's first day: all the period's, those ahead of today too. */
    val points: ImmutableList<LocalDate>,
    val income: ImmutableList<Double>?,
    val expenses: ImmutableList<Double>?,
    /** The day the lines get to: today, or the period's last when it's over. */
    val through: LocalDate,
    val before: PeriodBefore? = null,
)

/** The period before's line, all of it, and what it had come to as far into it as the period's got. */
@Immutable
data class PeriodBefore(
    val points: ImmutableList<LocalDate>,
    val addedUp: ImmutableList<Double>,
    val sameDay: LocalDate,
    val bySameDay: Double,
)

/** The step for a period of [days]: a day up to 3 months long, a month up to 3 years, then a year. */
internal fun chartStep(days: ClosedRange<LocalDate>): ChartStep = when {
    days.endInclusive <= days.start.plusMonths(DayStepsUpToMonths) -> ChartStep.Day
    days.endInclusive <= days.start.plusYears(MonthStepsUpToYears) -> ChartStep.Month
    else -> ChartStep.Year
}

/** Each step's first day in [days]: the first day, then each day, month or year that starts in them. */
internal fun chartPoints(days: ClosedRange<LocalDate>, step: ChartStep): List<LocalDate> =
    generateSequence(days.start) { point ->
        when (step) {
            ChartStep.Day -> point.plusDays(1)
            ChartStep.Month -> point.withDayOfMonth(1).plusMonths(1)
            ChartStep.Year -> point.withDayOfYear(1).plusYears(1)
        }
    }.takeWhile { it <= days.endInclusive }.toList()

/**
 * The period right before [days], as long: as many months when they're whole months, so that a
 * month's is the month before and a year's the year before, otherwise as many days.
 */
internal fun periodBefore(days: ClosedRange<LocalDate>): ClosedRange<LocalDate> {
    val after = days.endInclusive.plusDays(1)
    val months = ChronoUnit.MONTHS.between(days.start, after)
    val start = if (months > 0 && days.start.plusMonths(months) == after) {
        days.start.minusMonths(months)
    } else {
        days.start.minusDays(ChronoUnit.DAYS.between(days.start, after))
    }
    return start..days.start.minusDays(1)
}

/**
 * An account's chart of [days]: what came in and what went out, added up to [today], or the
 * period's last day when it's over. None before the period starts, or while nothing has moved.
 */
internal fun accountChart(days: ClosedRange<LocalDate>, today: LocalDate, flows: List<Flow>): PeriodChart? {
    if (today < days.start) return null
    val through = minOf(today, days.endInclusive)
    val sofar = flows.filter { it.day in days.start..through }
    if (sofar.none(Flow::moved)) return null
    val step = chartStep(days)
    val points = chartPoints(days, step)
    val last = points.indexOfLast { it <= through }
    return PeriodChart(
        step = step,
        points = points.toImmutableList(),
        income = addedUp(points, last, sofar, Flow::income).toImmutableList(),
        expenses = addedUp(points, last, sofar, Flow::expense).toImmutableList(),
        through = through,
    )
}

/**
 * A category's chart of [days]: what it took out, or brought in when it brings in more, added up to
 * [today], or the period's last day; over [before]'s, the period before's days and what moved in
 * them, when anything did. None before the period starts, or when nothing moved now or before.
 */
internal fun categoryChart(
    days: ClosedRange<LocalDate>,
    today: LocalDate,
    flows: List<Flow>,
    before: Pair<ClosedRange<LocalDate>, List<Flow>>?,
): PeriodChart? {
    if (today < days.start) return null
    val through = minOf(today, days.endInclusive)
    val sofar = flows.filter { it.day in days.start..through }
    val earlier = before?.second.orEmpty()
    val all = sofar + earlier
    if (all.none(Flow::moved)) return null
    val income = all.sumOf { it.income } > all.sumOf { it.expense }
    val value: (Flow) -> Double = if (income) Flow::income else Flow::expense
    val step = chartStep(days)
    val points = chartPoints(days, step)
    val line = addedUp(points, points.indexOfLast { it <= through }, sofar, value).toImmutableList()
    return PeriodChart(
        step = step,
        points = points.toImmutableList(),
        income = line.takeIf { income },
        expenses = line.takeIf { !income },
        through = through,
        before = before?.first?.takeIf { earlier.any { value(it) > 0 } }?.let { beforeDays ->
            val beforePoints = chartPoints(beforeDays, step)
            val sameDay = minOf(
                beforeDays.start.plusDays(ChronoUnit.DAYS.between(days.start, through)),
                beforeDays.endInclusive,
            )
            PeriodBefore(
                points = beforePoints.toImmutableList(),
                addedUp = addedUp(beforePoints, beforePoints.lastIndex, earlier, value).toImmutableList(),
                sameDay = sameDay,
                bySameDay = earlier.filter { it.day <= sameDay }.sumOf(value),
            )
        },
    )
}

/**
 * The days of the range: from its first, or, without one, the first day anything moved, [first];
 * to its last, or today without one. None without a first day.
 */
internal fun FromToTimeRange.chartDays(
    first: LocalDate?,
    today: LocalDate,
    converter: TimeConverter,
): ClosedRange<LocalDate>? = with(converter) {
    val start = from?.toLocalDate() ?: first ?: return null
    start..maxOf(start, to?.toLocalDate() ?: today)
}

/** The days as a range of time, from the first's start to the last's end. */
internal fun ClosedRange<LocalDate>.toTimeRange(converter: TimeConverter): FromToTimeRange = with(converter) {
    FromToTimeRange(
        from = start.atStartOfDay().toUTC(),
        to = endInclusive.plusDays(1).atStartOfDay().toUTC().minusNanos(1),
    )
}

// What [value] of the [flows] came to by the end of each of the [points], to the one at [last].
private fun addedUp(points: List<LocalDate>, last: Int, flows: List<Flow>, value: (Flow) -> Double): List<Double> =
    (0..last).map { index ->
        val next = points.getOrNull(index + 1)
        flows.filter { next == null || it.day < next }.sumOf(value)
    }

private const val DayStepsUpToMonths = 3L
private const val MonthStepsUpToYears = 3L
