package com.nivy.reports.trends

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Category
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.forecast.Band
import com.nivy.wallet.domain.pure.forecast.Shift
import com.nivy.wallet.domain.pure.forecast.bandsByStep
import com.nivy.wallet.domain.pure.forecast.reach
import com.nivy.wallet.domain.pure.forecast.shift
import com.nivy.wallet.domain.pure.forecast.simulateMonths
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.random.Random

/**
 * The net worth ahead, simulated from how it moved in the range's months: each month's band, and
 * when the next round amount is likely reached.
 */
@Immutable
data class NetWorthOutlook(val months: ImmutableList<Band>, val milestone: LikelyMilestone?)

/**
 * A round net worth ahead: the month it's [likely] reached by, and the [early] and [late] months
 * that 8 runs in 10 reach it between; [late] is null when more than 1 run in 10 takes over
 * [MilestoneHorizon] months.
 */
@Immutable
data class LikelyMilestone(val amount: Double, val likely: YearMonth, val early: YearMonth, val late: YearMonth?)

/** A category's spending over the range's full months, and how it shifted. */
@Immutable
data class HabitShift(
    val category: Category?,
    val values: ImmutableList<Double>,
    val periods: ImmutableList<TimePeriod>,
    val shift: Shift,
)

/**
 * The net worth's outlook from [start], now, in the month [now]: the months ahead move as the past
 * months' [changes], oldest first, did, the last ones' more often. Null with less than
 * [MinOutlookMonths] of them.
 */
internal fun netWorthOutlook(start: Double, changes: List<Double>, now: YearMonth, random: Random): NetWorthOutlook? {
    if (changes.size < MinOutlookMonths) return null
    val bands = bandsByStep(simulateMonths(start, changes, ForecastMonths, random))
    val milestone = nextRound(start)?.let { amount ->
        reach(simulateMonths(start, changes, MilestoneHorizon, random), amount)?.let { reached ->
            LikelyMilestone(
                amount = amount,
                likely = now.plusMonths(reached.likely.toLong()),
                early = now.plusMonths(reached.early.toLong()),
                late = reached.late?.let { now.plusMonths(it.toLong()) },
            )
        }
    }
    return NetWorthOutlook(bands.toImmutableList(), milestone)
}

/**
 * How the categories' spending shifted over the full ones of [months], their [spans] alike: those
 * that moved by [atLeast] a month and a fifth or more, the biggest moves first.
 */
internal fun habitShifts(
    months: List<MonthTrend>,
    spans: List<MonthSpan>,
    flows: List<Flow>,
    atLeast: Double,
): List<HabitShift> {
    val full = spans.zip(months).filter { (_, month) -> month.full }.map { it.first }
    val expenses = flows.filter { !it.income }
    return expenses.map { it.category }.distinct().mapNotNull { category ->
        val values = full.map { span ->
            val days = span.days.toSet()
            expenses.filter { it.category == category && it.day in days }.sumOf { it.amount }
        }
        shift(values, atLeast)?.let { shift ->
            HabitShift(category, values.toImmutableList(), full.map { it.period }.toImmutableList(), shift)
        }
    }.sortedByDescending { abs(it.shift.after - it.shift.before) }.take(ListedShifts)
}

// The next round amount above [netWorth]: a step of half its power of ten, 15,000 above 12,147.
private fun nextRound(netWorth: Double): Double? {
    if (netWorth < 1) return null
    val step = 10.0.pow(floor(log10(netWorth))) / 2
    return (floor(netWorth / step) + 1) * step
}

private const val MinOutlookMonths = 3

/** The complete months whose net worth changes the outlook draws on. */
internal const val OutlookMonths = 12
private const val MilestoneHorizon = 36
private const val ListedShifts = 3
