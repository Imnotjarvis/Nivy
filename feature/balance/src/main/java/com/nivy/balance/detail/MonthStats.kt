package com.nivy.balance.detail

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * A purchase, in the base currency: the [day] it counts on, the moment it was [made] (for a card,
 * when it was bought, not when it's charged), and what it was. [recurring] when a planned payment
 * made it: a bill or a subscription.
 */
@Immutable
internal data class Spend(
    val amount: Double,
    val day: LocalDate,
    val made: LocalDateTime,
    val title: String?,
    val category: Category?,
    val account: Account?,
    val recurring: Boolean,
)

/** Purchase sizes, in euros: under 5, 5 to 15, 15 to 50, 50 to 150, and more. */
internal val SizeEdgesInEuros = listOf(5.0, 15.0, 50.0, 150.0)

// The edge under which a purchase is small: 15 euros.
private const val SmallEdge = 1

/** [SizeEdgesInEuros] in a currency of which a euro is [perEuro], rounded to round amounts. */
internal fun sizeEdges(perEuro: Double): List<Double> =
    if (perEuro == 1.0) SizeEdgesInEuros else SizeEdgesInEuros.map { roundNicely(it * perEuro) }

/** The nearest of 1, 2, 2.5 and 5 times a power of ten. */
internal fun roundNicely(value: Double): Double {
    if (value <= 0) return value
    val power = 10.0.pow(floor(log10(value)))
    return NiceSteps.map { it * power }.minBy { abs(it - value) }
}

private val NiceSteps = listOf(1.0, 2.0, 2.5, 5.0, 10.0)

// The weekend, as most calendars have it.
private val Weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

// The parts of the day, from the hour each starts: morning, afternoon, evening, night.
private val DayParts = listOf(6 until 12, 12 until 18, 18 until 24, 0 until 6)

// At most this many categories and places are listed.
private const val ListedCategories = 6
private const val ListedPlaces = 4

/**
 * How the money went, from the month's [spends] over its [elapsed] days, or null without any.
 * [outgoing] is all that went out, purchases or not, and [income] all that came in; the planned
 * expenses still due count as fixed, as the bills and subscriptions already paid do. [previous] is
 * what each category took over as many days of the month before.
 */
@Suppress("LongParameterList")
internal fun habits(
    spends: List<Spend>,
    elapsed: List<LocalDate>,
    daysInMonth: Int,
    income: Double,
    outgoing: Double,
    plannedExpenses: List<Double>,
    previous: Map<Category?, Double>,
    edges: List<Double>,
): Habits? {
    if (spends.isEmpty() || elapsed.isEmpty()) return null
    val spent = spends.sumOf { it.amount }
    val amounts = spends.map { it.amount }.sorted()
    val byWeekday = DayOfWeek.entries.map { weekday ->
        spends.filter { it.made.dayOfWeek == weekday }.sumOf { it.amount }
    }
    val spentOnDay = elapsed.associateWith { day -> spends.filter { it.day == day }.sumOf { it.amount } }
    val small = spends.filter { it.amount < edges[SmallEdge] }
    val recurring = spends.filter { it.recurring }
    val places = places(spends)
    return Habits(
        spent = spent,
        days = elapsed.size,
        daysInMonth = daysInMonth,
        savedShare = if (income > 0) (income - outgoing) / income else null,
        biggest = spends.maxBy { it.amount }.let { Biggest(it.amount, it.title, it.category, it.day) },
        purchases = spends.size,
        median = median(amounts),
        average = spent / spends.size,
        byWeekday = byWeekday.toImmutableList(),
        busiest = DayOfWeek.entries[byWeekday.indexOf(byWeekday.max())],
        weekendRatio = weekendRatio(spends, elapsed),
        noSpendDays = spentOnDay.values.count { it == 0.0 },
        small = Small(count = small.size, share = small.sumOf { it.amount } / spent, under = edges[SmallEdge]),
        topPlace = places.firstOrNull(),
        topAccount = spends.groupBy { it.account }.maxBy { it.value.size }.let { AccountUse(it.key, it.value.size) },
        timeOfDay = timeOfDay(spends),
        sizes = sizeBands(spends, spent, edges).toImmutableList(),
        fixed = Portion(recurring.sumOf { it.amount } + plannedExpenses.sum(), recurring.size + plannedExpenses.size),
        flexible = Portion(spent - recurring.sumOf { it.amount }, spends.size - recurring.size),
        categories = categoryChanges(spends, previous).toImmutableList(),
        places = places.take(ListedPlaces).toImmutableList(),
    )
}

private fun median(sorted: List<Double>): Double {
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

// A weekend day's spending over a weekday's, each on average over the days of its kind so far.
private fun weekendRatio(spends: List<Spend>, elapsed: List<LocalDate>): Double? {
    val weekendDays = elapsed.count { it.dayOfWeek in Weekend }
    val weekdays = elapsed.size - weekendDays
    if (weekendDays == 0 || weekdays == 0) return null
    val weekend = spends.filter { it.made.dayOfWeek in Weekend }.sumOf { it.amount } / weekendDays
    val weekday = spends.filter { it.made.dayOfWeek !in Weekend }.sumOf { it.amount } / weekdays
    return if (weekday > 0) weekend / weekday else null
}

// Purchases made at midnight sharp have no time of their own, as imported ones often don't: they
// stay out, and without any timed purchase there's no time of day to tell.
private fun timeOfDay(spends: List<Spend>): ImmutableList<Double>? {
    val timed = spends.filter { it.made.toLocalTime() != LocalTime.MIDNIGHT }
    val total = timed.sumOf { it.amount }
    if (total <= 0) return null
    return DayParts.map { hours -> timed.filter { it.made.hour in hours }.sumOf { it.amount } / total }
        .toImmutableList()
}

private fun sizeBands(spends: List<Spend>, spent: Double, edges: List<Double>): List<SizeBand> {
    val bounds = listOf(0.0) + edges
    return bounds.mapIndexed { index, from ->
        val to = edges.getOrNull(index)
        val inBand = spends.filter { it.amount >= from && (to == null || it.amount < to) }
        SizeBand(from = from, to = to, count = inBand.size, share = inBand.sumOf { it.amount } / spent)
    }
}

// The month's categories, the largest first, each with its change from the month before.
private fun categoryChanges(spends: List<Spend>, previous: Map<Category?, Double>): List<CategoryChange> =
    spends.groupBy { it.category }
        .map { (category, inCategory) ->
            val amount = inCategory.sumOf { it.amount }
            val before = previous[category]
            CategoryChange(category, amount, if (before != null && before > 0) (amount - before) / before else null)
        }
        .sortedByDescending { it.amount }
        .take(ListedCategories)

// The places gone back to, by the purchases' titles, whatever their case: the most visited first.
private fun places(spends: List<Spend>): List<Place> =
    spends.filter { !it.title.isNullOrBlank() }
        .groupBy { it.title.orEmpty().trim().lowercase() }
        .values
        .filter { it.size > 1 }
        .map { visits ->
            val name = visits.groupBy { it.title.orEmpty().trim() }.maxBy { it.value.size }.key
            Place(name = name, visits = visits.size, total = visits.sumOf { it.amount })
        }
        .sortedWith(compareByDescending<Place> { it.visits }.thenByDescending { it.total })

/**
 * What can be spent a day for the rest of the month to end even, or null when nothing comes in
 * this month or no day is left.
 */
internal fun freeToSpend(
    income: Double,
    plannedIncome: Double,
    spent: Double,
    plannedExpenses: Double,
    daysLeft: Int,
): FreeToSpend? {
    if (daysLeft <= 0 || income + plannedIncome <= 0) return null
    val left = income + plannedIncome - spent - plannedExpenses
    return FreeToSpend(perDay = left / daysLeft, daysLeft = daysLeft, spent = spent, left = left)
}

/** The lowest of the [balances] planned after the [actualDays], when it's below the last actual one. */
internal fun lowestAhead(days: List<LocalDate>, balances: List<Double>, actualDays: Int): DayAmount? {
    if (actualDays == 0 || actualDays >= balances.size) return null
    val lowest = (actualDays until balances.size).minBy { balances[it] }
    return if (balances[lowest] < balances[actualDays - 1]) DayAmount(days[lowest], balances[lowest]) else null
}

/** How long [netWorth] lasts at the average of the months that had spending, or null. */
internal fun runway(netWorth: Double, monthlySpending: List<Double>): Runway? {
    val months = monthlySpending.filter { it > 0 }
    if (netWorth <= 0 || months.isEmpty()) return null
    val monthly = months.average()
    return Runway(months = netWorth / monthly, monthly = monthly, over = months.size)
}
