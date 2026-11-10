package com.nivy.wallet.domain.pure.spending

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A paid income or expense in the base currency, as the stats screens count it: the [day] it
 * counts on, the moment it was [made] (for a card, when it was bought, not when it's charged), and
 * what it was. [purchase] for an expense that isn't a loan's money; [recurring] when a planned
 * payment made it: a bill or a subscription.
 */
@Immutable
data class Flow(
    val amount: Double,
    val income: Boolean,
    val day: LocalDate,
    val made: LocalDateTime,
    val title: String?,
    val category: Category?,
    val account: Account?,
    val recurring: Boolean,
    val purchase: Boolean,
) {
    /**
     * A purchase made by choice: a planned payment's bill or subscription is paid when it's due, on
     * whatever day and at whatever time, and would take over the patterns.
     */
    val habit: Boolean get() = purchase && !recurring

    /** Made at a time of its own: one at midnight sharp has none, as imported ones often don't. */
    val timed: Boolean get() = made.toLocalTime() != LocalTime.MIDNIGHT
}

/** A day, and an amount on it. */
@Immutable
data class DayAmount(val day: LocalDate, val amount: Double)

/** The biggest purchase of a time: what it was, and the day it counts on. */
@Immutable
data class BiggestPurchase(val amount: Double, val title: String?, val category: Category?, val day: LocalDate)

/** The weekend, as most calendars have it. */
val Weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** The parts of the day, from the hour each starts: morning, afternoon, evening, night. */
val DayParts = listOf(6 until 12, 12 until 18, 18 until 24, 0 until 6)

/** The evening, among the [DayParts]. */
const val EveningPart = 2

/** The middle of [values], or of the two in the middle; null without any. */
fun median(values: List<Double>): Double? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

/** The value [share] of the way through the [sorted] values, between neighbors; 0 without any. */
fun percentile(sorted: List<Double>, share: Double): Double {
    if (sorted.isEmpty()) return 0.0
    val at = share * (sorted.size - 1)
    val below = floor(at).toInt()
    val above = ceil(at).toInt()
    return sorted[below] + (sorted[above] - sorted[below]) * (at - below)
}

// The rules every stats screen counts by: a tile with the same name counts the same things on each.

/** The purchases made by choice: what the habit tiles count, without bills, subscriptions or loans' money. */
fun List<Flow>.chosen(): List<Flow> = filter { it.habit }

/** The fixed part of the purchases: the bills and subscriptions planned payments made. Loans' money isn't one. */
fun List<Flow>.fixed(): List<Flow> = filter { it.purchase && it.recurring }

/** The days from [since], the day of the first paid transaction, on: those before weren't tracked. */
fun List<LocalDate>.trackedSince(since: LocalDate?): List<LocalDate> =
    if (since == null) this else filter { !it.isBefore(since) }

/** The [days] without a purchase made by choice: a day with only a bill or a subscription is one. */
fun noSpendDays(days: List<LocalDate>, flows: List<Flow>): Int {
    val spendDays = flows.chosen().map { it.day }.toSet()
    return days.count { it !in spendDays }
}
