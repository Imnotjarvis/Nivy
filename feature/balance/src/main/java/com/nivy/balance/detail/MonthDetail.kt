package com.nivy.balance.detail

import androidx.compose.runtime.Immutable
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.domain.pure.spending.BiggestPurchase
import com.nivy.wallet.domain.pure.spending.DayAmount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The net worth screen's month in detail, amounts in the base currency: its balance day by day,
 * what's still to come, where the money went and how, and what's owned and owed now.
 */
@Immutable
data class MonthDetail(
    val days: ImmutableList<LocalDate>,
    // Today, when it's one of [days].
    val today: LocalDate?,
    // Each day's closing balance: as it was for the first [actualDays], as planned after.
    val balances: ImmutableList<Double>,
    val actualDays: Int,
    // How the balance moved since the month began, while it's the current month.
    val sinceStart: Double?,
    // The lowest balance planned ahead, when it's below today's.
    val lowest: DayAmount?,
    val freeToSpend: FreeToSpend?,
    val comingUp: ImmutableList<Upcoming>,
    val moreComingUp: Int,
    // What was spent each of [days], none on the days still to come.
    val spentByDay: ImmutableList<Double>,
    // The days ahead with an expense planned.
    val plannedDays: ImmutableSet<LocalDate>,
    val pace: Pace?,
    val habits: Habits?,
    val holdings: Holdings,
    val runway: Runway?,
    // What the rest of the month likely holds, while it's the current month.
    val outlook: MonthOutlook? = null,
    // What stands out in the month so far.
    val unusual: Unusual? = null,
) {
    companion object {
        val Empty = MonthDetail(
            days = persistentListOf(),
            today = null,
            balances = persistentListOf(),
            actualDays = 0,
            sinceStart = null,
            lowest = null,
            freeToSpend = null,
            comingUp = persistentListOf(),
            moreComingUp = 0,
            spentByDay = persistentListOf(),
            plannedDays = persistentSetOf(),
            pace = null,
            habits = null,
            holdings = Holdings(persistentListOf(), persistentListOf(), 0.0),
            runway = null,
        )
    }
}

/**
 * What can still be spent each of the [daysLeft] days for the month to end even: what came in
 * and is still due to, less what was spent and is still due to be paid. [left] is below zero once
 * more went out than came in.
 */
@Immutable
data class FreeToSpend(val perDay: Double, val daysLeft: Int, val spent: Double, val left: Double)

/** A planned payment still to come: income above zero, an expense below. */
@Immutable
data class Upcoming(
    val day: LocalDate,
    val title: String?,
    val category: Category?,
    val account: Account?,
    val type: TransactionType,
    val amount: Double,
)

/**
 * What was spent by each day so far, and by each day of the month before, added up; the day of the
 * month before that today compares with.
 */
@Immutable
data class Pace(
    val current: ImmutableList<Double>,
    val previous: ImmutableList<Double>,
    val previousMonth: Month?,
    val comparedTo: LocalDate?,
)

/** How the money went, from the month's purchases so far. */
@Immutable
data class Habits(
    // Every purchase's, bills and subscriptions included.
    val spent: Double,
    // The purchases made by choice's, which the habit tiles' shares are of.
    val chosenSpent: Double,
    val days: Int,
    // The days so far from the first paid transaction's, which the no-spend days count over.
    val trackedDays: Int,
    // Of what came in, the share not spent; null when nothing came in.
    val savedShare: Double?,
    val biggest: BiggestPurchase?,
    val purchases: Int,
    // Of the purchases made by choice: none without any.
    val median: Double?,
    val average: Double?,
    // What was spent on each weekday, Monday first.
    val byWeekday: ImmutableList<Double>,
    val busiest: DayOfWeek?,
    // A weekend day's spending over a weekday's, on average; null without both.
    val weekendRatio: Double?,
    val noSpendDays: Int,
    val small: Small,
    val topPlace: Place?,
    val topAccount: AccountUse?,
    // The shares of the morning, afternoon, evening and night, of the purchases with a time.
    val timeOfDay: ImmutableList<Double>?,
    val sizes: ImmutableList<SizeBand>,
    val fixed: Portion,
    val flexible: Portion,
    val categories: ImmutableList<CategoryChange>,
    val places: ImmutableList<Place>,
) {
    val perDay: Double get() = if (days > 0) spent / days else 0.0
}

/** The purchases [under] an amount: how many, and their share of the spending. */
@Immutable
data class Small(val count: Int, val share: Double, val under: Double)

/** A place, by a purchase's title: how often, and how much in all. */
@Immutable
data class Place(val name: String, val visits: Int, val total: Double)

@Immutable
data class AccountUse(val account: Account?, val payments: Int)

/** Purchases from [from] up to [to] (none: no limit): how many, and their share of the spending. */
@Immutable
data class SizeBand(val from: Double, val to: Double?, val count: Int, val share: Double)

@Immutable
data class Portion(val amount: Double, val count: Int)

/** A category's spending so far, and its change from as many days the month before; null if new. */
@Immutable
data class CategoryChange(val category: Category?, val amount: Double, val change: Double?)

/** The accounts in the balance, as now: what's owned, what's owed, and the net worth. */
@Immutable
data class Holdings(
    val assets: ImmutableList<Holding>,
    val debts: ImmutableList<Holding>,
    val netWorth: Double,
)

@Immutable
data class Holding(val account: Account, val amount: Double)

/** How many [months] the net worth lasts at the [monthly] spending of the last months. */
@Immutable
data class Runway(val months: Double, val monthly: Double, val over: Int)
