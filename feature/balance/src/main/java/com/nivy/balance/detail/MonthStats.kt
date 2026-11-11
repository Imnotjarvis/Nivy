package com.nivy.balance.detail

import com.nivy.data.model.Category
import com.nivy.wallet.domain.pure.spending.BiggestPurchase
import com.nivy.wallet.domain.pure.spending.DayAmount
import com.nivy.wallet.domain.pure.spending.DayParts
import com.nivy.wallet.domain.pure.spending.Flow
import com.nivy.wallet.domain.pure.spending.Weekend
import com.nivy.wallet.domain.pure.spending.chosen
import com.nivy.wallet.domain.pure.spending.fixed
import com.nivy.wallet.domain.pure.spending.median
import com.nivy.wallet.domain.pure.spending.noSpendDays
import com.nivy.wallet.domain.pure.spending.trackedSince
import com.nivy.wallet.domain.pure.util.SmallPurchaseEdgeIndex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.DayOfWeek
import java.time.LocalDate

// At most this many categories are listed.
private const val ListedCategories = 6

/**
 * How the money went, from the month's [spends] over its [elapsed] days, or null without any.
 * The habit tiles count the purchases made by choice, as Trends does, and the days without
 * spending from [trackedSince], the first paid transaction's day; what was spent, the fixed part
 * and the categories count every purchase. [outgoing] is all that went out, purchases or not, and
 * [income] all that came in; [plannedFixed], the bills and subscriptions still due, count as fixed
 * as those already paid do. [previous] is what each category took over as many days of the month
 * before.
 */
internal fun habits(
    spends: List<Flow>,
    elapsed: List<LocalDate>,
    trackedSince: LocalDate?,
    income: Double,
    outgoing: Double,
    plannedFixed: List<Double>,
    previous: Map<Category?, Double>,
    edges: List<Double>,
): Habits? {
    val spent = spends.sumOf { it.amount }
    // Nothing spent tells no habits: every share of it would be 0 over 0.
    if (spent <= 0 || elapsed.isEmpty()) return null
    val chosen = spends.chosen()
    val chosenSpent = chosen.sumOf { it.amount }
    val byWeekday = DayOfWeek.entries.map { weekday ->
        chosen.filter { it.made.dayOfWeek == weekday }.sumOf { it.amount }
    }
    val small = chosen.filter { it.amount < edges[SmallPurchaseEdgeIndex] }
    val fixed = spends.fixed()
    val tracked = elapsed.trackedSince(trackedSince)
    return Habits(
        spent = spent,
        chosenSpent = chosenSpent,
        days = elapsed.size,
        trackedDays = tracked.size,
        savedShare = if (income > 0) (income - outgoing) / income else null,
        biggest = spends.maxBy { it.amount }.let { BiggestPurchase(it.amount, it.title, it.category, it.day) },
        purchases = chosen.size,
        median = median(chosen.map { it.amount }),
        average = if (chosen.isEmpty()) null else chosenSpent / chosen.size,
        byWeekday = byWeekday.toImmutableList(),
        busiest = if (chosenSpent > 0) DayOfWeek.entries[byWeekday.indexOf(byWeekday.max())] else null,
        weekendRatio = weekendRatio(chosen, elapsed),
        noSpendDays = noSpendDays(tracked, chosen),
        small = Small(
            count = small.size,
            share = if (chosenSpent > 0) small.sumOf { it.amount } / chosenSpent else 0.0,
            under = edges[SmallPurchaseEdgeIndex]
        ),
        topPlace = places(chosen).firstOrNull(),
        topAccount = chosen.groupBy { it.account }
            .maxByOrNull { it.value.size }
            ?.let { AccountUse(it.key, it.value.size) },
        timeOfDay = timeOfDay(chosen),
        sizes = sizeBands(chosen, chosenSpent, edges).toImmutableList(),
        fixed = Portion(fixed.sumOf { it.amount } + plannedFixed.sum(), fixed.size + plannedFixed.size),
        flexible = Portion(chosenSpent, chosen.size),
        categories = categoryChanges(spends, previous).toImmutableList(),
    )
}

// A weekend day's spending over a weekday's, each on average over the days of its kind so far.
private fun weekendRatio(spends: List<Flow>, elapsed: List<LocalDate>): Double? {
    val weekendDays = elapsed.count { it.dayOfWeek in Weekend }
    val weekdays = elapsed.size - weekendDays
    if (weekendDays == 0 || weekdays == 0) return null
    val weekend = spends.filter { it.made.dayOfWeek in Weekend }.sumOf { it.amount } / weekendDays
    val weekday = spends.filter { it.made.dayOfWeek !in Weekend }.sumOf { it.amount } / weekdays
    return if (weekday > 0) weekend / weekday else null
}

// Purchases made at midnight sharp have no time of their own, as imported ones often don't: they
// stay out, and without any timed purchase there's no time of day to tell.
private fun timeOfDay(spends: List<Flow>): ImmutableList<Double>? {
    val timed = spends.filter { it.timed }
    val total = timed.sumOf { it.amount }
    if (total <= 0) return null
    return DayParts.map { hours -> timed.filter { it.made.hour in hours }.sumOf { it.amount } / total }
        .toImmutableList()
}

private fun sizeBands(spends: List<Flow>, spent: Double, edges: List<Double>): List<SizeBand> {
    val bounds = listOf(0.0) + edges
    return bounds.mapIndexed { index, from ->
        val to = edges.getOrNull(index)
        val inBand = spends.filter { it.amount >= from && (to == null || it.amount < to) }
        val share = if (spent > 0) inBand.sumOf { it.amount } / spent else 0.0
        SizeBand(from = from, to = to, count = inBand.size, share = share)
    }
}

// The month's categories, the largest first, each with its change from the month before.
private fun categoryChanges(spends: List<Flow>, previous: Map<Category?, Double>): List<CategoryChange> =
    spends.groupBy { it.category }
        .map { (category, inCategory) ->
            val amount = inCategory.sumOf { it.amount }
            val before = previous[category]
            CategoryChange(category, amount, if (before != null && before > 0) (amount - before) / before else null)
        }
        .sortedByDescending { it.amount }
        .take(ListedCategories)

// The places gone back to, by the purchases' titles, whatever their case: the most visited first.
private fun places(spends: List<Flow>): List<Place> =
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
 * What can be spent a day for the rest of the month to stay within the [budget], once what's
 * [spent] and the [plannedExpenses] still due are taken from it, or null when no day is left.
 */
internal fun freeToSpend(budget: Double, spent: Double, plannedExpenses: Double, daysLeft: Int): FreeToSpend? {
    if (daysLeft <= 0) return null
    val left = budget - spent - plannedExpenses
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
