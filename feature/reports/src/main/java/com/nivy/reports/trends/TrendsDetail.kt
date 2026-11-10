package com.nivy.reports.trends

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.spending.BiggestPurchase
import com.nivy.wallet.domain.pure.spending.DayAmount
import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** How far back the trends look: a number of months, or all of them. */
enum class TrendsRange(val months: Int?) {
    ThreeMonths(3),
    SixMonths(6),
    Year(12),

    // From the first month with a paid transaction, up to [AllMonthsAtMost] of them.
    All(null),
}

/** The most months "all" goes back: more would draw bars too thin to read. */
const val AllMonthsAtMost = 60

/**
 * The trends of a range of months, oldest first and the current month last, in the base
 * currency. Income, expenses and habits are the accounts in the balance's, as Home counts them;
 * the net worth is every account's, as the Accounts tab counts it.
 */
@Immutable
data class TrendsDetail(
    val months: ImmutableList<MonthTrend>,
    // The net worth for the next months, at the range's pace.
    val forecast: ImmutableList<Double>,
    val milestone: Milestone?,
    // What each account held at each month's end, the largest first, then the others together.
    val accounts: ImmutableList<AccountSeries>,
    // What each category took each month, the largest first, then the others together.
    val categories: ImmutableList<CategorySeries>,
    // The categories that changed the most, the last 3 months against the 3 before.
    val changes: ImmutableList<CategoryShift>,
    val habits: RangeHabits,
    // What's spent on average on each weekday, Monday first, in the morning, afternoon, evening and
    // night; null without a purchase with a time.
    val punchCard: ImmutableList<ImmutableList<Double>>?,
    val calendar: SpendingCalendar?,
    val records: Records,
    // The net worth ahead, simulated from the range's months.
    val outlook: NetWorthOutlook? = null,
    // How categories' spending shifted over the range's full months, the biggest shifts first.
    val shifts: ImmutableList<HabitShift> = kotlinx.collections.immutable.persistentListOf(),
)

/** A month of the trends. */
@Immutable
data class MonthTrend(
    val period: TimePeriod,
    // Whether the month is over: the current month isn't.
    val complete: Boolean,
    // Whether it's over and the app was used all of it, so that its counts compare with others'.
    val full: Boolean,
    val income: Double,
    val expense: Double,
    // At the month's end, or now for the current month.
    val netWorth: Double,
    // Of the purchases, the bills and subscriptions that planned payments made: loans' money isn't one.
    val fixed: Double,
    val habits: MonthHabits,
) {
    val saved: Double get() = income - expense

    // Of the purchases, those made by choice: with [fixed], what fixed and flexible split.
    val flexible: Double get() = habits.spent

    // Of the income, the share saved; null without income.
    val savingsRate: Double? get() = if (income > 0) saved / income else null
}

/**
 * A month's purchases made by choice, the bills and subscriptions of planned payments left out:
 * how many, how big, on which days and at which times of day.
 */
@Immutable
data class MonthHabits(
    val purchases: Int,
    val spent: Double,
    // The median purchase; null without any.
    val typical: Double?,
    // Of the month's days so far, those without a purchase.
    val noSpendDays: Int,
    val days: Int,
    // Spent on Saturdays and Sundays.
    val weekend: Double,
    // Spent in small purchases.
    val small: Double,
    // Spent in the evening, and in all, by the purchases with a time.
    val evening: Double,
    val timed: Double,
)

/** A round net worth ahead, and the month it's reached at the range's pace. */
@Immutable
data class Milestone(val amount: Double, val by: YearMonth)

/** An account's balance at each month's end; null for the other accounts together. */
@Immutable
data class AccountSeries(val account: Account?, val values: ImmutableList<Double>)

/** What a category took each month; [other] for the other categories together. */
@Immutable
data class CategorySeries(val category: Category?, val other: Boolean, val values: ImmutableList<Double>)

/**
 * A category's monthly average of the last 3 months, the 3 before's, and the change from it; null
 * when it's new.
 */
@Immutable
data class CategoryShift(val category: Category?, val average: Double, val before: Double, val change: Double?)

/** The range's habits as a whole. */
@Immutable
data class RangeHabits(
    // The median purchase; null without any.
    val typical: Double?,
    // Of the spending, the shares on weekends, in small purchases and, of that with a time, in the
    // evening; null without spending.
    val weekendShare: Double?,
    val smallShare: Double?,
    val eveningShare: Double?,
    // Of the days so far, the share of weekend days.
    val weekendDays: Double,
    // The amount under which a purchase is small.
    val smallUnder: Double,
)

/**
 * The days of the last weeks, a column a week from Monday: what was spent each day, null for one
 * outside the range or still to come. [levels] are the amounts from which a day is darker.
 */
@Immutable
data class SpendingCalendar(
    val start: LocalDate,
    val amounts: ImmutableList<Double?>,
    val levels: ImmutableList<Double>,
    val top: DayAmount?,
)

/** The range's records; each null without one. */
@Immutable
data class Records(
    val mostSaved: MonthAmount?,
    val mostSpent: MonthAmount?,
    val biggest: BiggestPurchase?,
    val noSpendStreak: DaySpan?,
    val cheapestWeek: WeekAmount?,
    val busiestDay: DayCount?,
)

@Immutable
data class MonthAmount(val period: TimePeriod, val amount: Double)

/** The days from [from] to [to], both counted. */
@Immutable
data class DaySpan(val from: LocalDate, val to: LocalDate) {
    val days: Int get() = ChronoUnit.DAYS.between(from, to).toInt() + 1
}

/** What a week from Monday [from] to Sunday cost. */
@Immutable
data class WeekAmount(val from: LocalDate, val amount: Double)

@Immutable
data class DayCount(val day: LocalDate, val purchases: Int)
