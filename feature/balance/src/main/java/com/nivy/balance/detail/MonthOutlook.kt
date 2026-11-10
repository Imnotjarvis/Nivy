package com.nivy.balance.detail

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Category
import com.nivy.wallet.domain.pure.forecast.Band
import com.nivy.wallet.domain.pure.forecast.band
import com.nivy.wallet.domain.pure.forecast.bandsByStep
import com.nivy.wallet.domain.pure.forecast.simulateDays
import com.nivy.wallet.domain.pure.spending.Flow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

/**
 * What the rest of the month likely holds, from everyday spending simulated after the last weeks':
 * the balance each day ahead, its likely lowest, what the month's purchases come to by each day,
 * and how much of what's free to spend is likely left.
 */
@Immutable
data class MonthOutlook(
    // For today and each day after it: its closing balance as planned, less the likely spending.
    val balances: ImmutableList<Band>,
    val lowest: LikelyLowest?,
    // For today and each day after it: the month's purchases so far, planned and likely by then.
    val spending: ImmutableList<Band>,
    // What a day ahead likely spends, on average.
    val perDay: Double,
    // Of what's free to spend, what's likely left at the month's end; null when nothing is free.
    val left: Band?,
)

/** The lowest balance ahead: [likely] around [day], and as low as [low] in a busier month. */
@Immutable
data class LikelyLowest(val day: LocalDate, val likely: Double, val low: Double)

/**
 * The month's outlook from today, the last of its [actualDays]: [balances] as planned, the
 * purchases [spentSoFar], the [planned] purchases due by today and each day after it, and
 * [history], the everyday spending of each past day counted. Null when the month is over, or with
 * less than [MinHistoryDays] days of history.
 */
internal fun monthOutlook(
    days: List<LocalDate>,
    actualDays: Int,
    balances: List<Double>,
    spentSoFar: Double,
    planned: List<Double>,
    history: Map<LocalDate, Double>,
    free: FreeToSpend?,
    random: Random,
): MonthOutlook? {
    if (actualDays == 0 || actualDays >= days.size || history.size < MinHistoryDays) return null
    val today = actualDays - 1
    val ahead = days.subList(actualDays, days.size)
    val runs = simulateDays(history, ahead, random)
    // Today first, spending nothing more, so that the band opens at today's balance.
    val spent = listOf(Band(0.0, 0.0, 0.0)) + bandsByStep(runs)
    val likely = spent.mapIndexed { step, band -> band.takenFrom(balances[today + step]) }
    val lowest = likely.indices.minBy { likely[it].likely }
    val lows = DoubleArray(runs.size) { run ->
        minOf(balances[today], runs[run].indices.minOf { balances[actualDays + it] - runs[run][it] })
    }
    val total = spent.last()
    return MonthOutlook(
        balances = likely.toImmutableList(),
        lowest = LikelyLowest(days[today + lowest], likely[lowest].likely, band(lows).low).takeIf { lowest > 0 },
        spending = spent.mapIndexed { step, band -> band + (spentSoFar + planned[step]) }.toImmutableList(),
        perDay = total.likely / ahead.size,
        left = free?.takeIf { it.left > 0 }?.let { total.takenFrom(it.left) },
    )
}

/** What stands out in the month so far against the months before: categories, then purchases. */
@Immutable
data class Unusual(
    val categories: ImmutableList<UnusualCategory>,
    val purchases: ImmutableList<UnusualPurchase>,
)

/**
 * A category's everyday spending so far, against what it was by the same day of each of the
 * [months] before: more than in [beaten] of them when [higher], less than in [beaten] otherwise.
 * [usual] is their median.
 */
@Immutable
data class UnusualCategory(
    val category: Category?,
    val amount: Double,
    val usual: Double,
    val beaten: Int,
    val months: Int,
    val higher: Boolean,
)

/** A purchase bigger than any of its category's [since] a month, or in all [months] before when null. */
@Immutable
data class UnusualPurchase(
    val title: String?,
    val category: Category?,
    val amount: Double,
    val day: LocalDate,
    val since: YearMonth?,
    val months: Int,
)

/** A month before: its [days], and its [spends]. */
internal data class PastMonth(val month: YearMonth, val days: List<LocalDate>, val spends: List<Flow>)

/**
 * What stands out in the month's [spends] by its [elapsed] days, against the [past] months, oldest
 * first, everyday purchases only: purchases of 3 times [atLeast] or more bigger than any of their
 * category's for [MinMonthsSince] months or more; then categories that took more, or less, than by
 * the same day in 9 months in 10, by 3 times [atLeast] and a quarter or more, unless one of those
 * purchases makes most of it. Null with less than [MinPastMonths] months, or nothing to tell.
 */
internal fun unusual(spends: List<Flow>, elapsed: Int, past: List<PastMonth>, atLeast: Double): Unusual? {
    if (past.size < MinPastMonths) return null
    val everyday = spends.filter { !it.recurring }
    val earlier = past.flatMap { it.spends }.filter { !it.recurring }
    val purchases = everyday.filter { it.amount >= Big * atLeast }.mapNotNull { spend ->
        val inCategory = earlier.filter { it.category == spend.category }
        if (inCategory.size < MinPurchases) return@mapNotNull null
        val since = inCategory.filter { it.amount >= spend.amount }.maxOfOrNull { it.day }?.let(YearMonth::from)
        val long = since == null || !since.isAfter(YearMonth.from(spend.day).minusMonths(MinMonthsSince))
        if (long) UnusualPurchase(spend.title, spend.category, spend.amount, spend.day, since, past.size) else null
    }.sortedByDescending { it.amount }.take(ListedPurchases)

    val needed = ceil(past.size * RareShare).toInt()
    val categories = everyday.groupBy { it.category }.mapNotNull { (category, inCategory) ->
        val amount = inCategory.sumOf { it.amount }
        if (purchases.any { it.category == category && it.amount > amount / 2 }) return@mapNotNull null
        val before = past.map { month ->
            val sameDays = month.days.take(elapsed).toSet()
            month.spends.filter { !it.recurring && it.category == category && it.day in sameDays }.sumOf { it.amount }
        }
        val usual = before.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }
        val apart = abs(amount - usual) >= maxOf(Big * atLeast, ApartShare * usual)
        val less = before.count { it < amount }
        val more = before.count { it > amount }
        when {
            apart && amount > usual && less >= needed ->
                UnusualCategory(category, amount, usual, less, past.size, higher = true)
            apart && amount < usual && more >= needed ->
                UnusualCategory(category, amount, usual, more, past.size, higher = false)
            else -> null
        }
    }.sortedWith(compareByDescending<UnusualCategory> { it.higher }.thenByDescending { abs(it.amount - it.usual) })
        .take(ListedUnusual)

    if (categories.isEmpty() && purchases.isEmpty()) return null
    return Unusual(categories.toImmutableList(), purchases.toImmutableList())
}

// Less than this many days of history, and the month has no outlook.
private const val MinHistoryDays = 28

private const val MinPastMonths = 6

// A category stands out by beating 9 months in 10, by a quarter of the usual or more.
private const val RareShare = 0.9
private const val ApartShare = 0.25

private const val Big = 3
private const val MinPurchases = 8
private const val MinMonthsSince = 4L
private const val ListedUnusual = 3
private const val ListedPurchases = 2
