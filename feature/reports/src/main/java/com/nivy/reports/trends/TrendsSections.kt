package com.nivy.reports.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.ui.R
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.component.GroupTitle
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.StatTiles
import com.nivy.wallet.ui.component.dayLabel
import com.nivy.wallet.ui.component.evenly
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.ui.component.weekdayName
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.utils.format
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.abs

/**
 * The trends under the net worth: the net worth, money in and out, spending, habits and records,
 * a group each. Tapping or dragging across a chart picks the month every chart marks.
 */
@Composable
internal fun ColumnScope.TrendsSections(
    detail: TrendsDetail,
    range: TrendsRange,
    selected: Int,
    currency: String,
    onSelect: (Int) -> Unit,
) {
    val months = detail.months
    val axis = monthAxis(months.map { it.period })
    val phrase = rangePhrase(range, months.first().period)

    GroupTitle(stringResource(R.string.net_worth))
    NetWorthPanel(detail, range, selected, currency, onSelect)
    if (detail.accounts.isNotEmpty() && months.size > 1) {
        Spacer(Modifier.height(16.dp))
        AccountsPanel(detail.accounts, axis, selected, onSelect)
    }

    GroupTitle(stringResource(R.string.trends_money_in_and_out))
    IncomeExpensesPanel(months, axis, selected, currency, onSelect)
    Spacer(Modifier.height(16.dp))
    SavedPanel(months, axis, selected, currency, onSelect)
    if (months.any { it.savingsRate != null }) {
        Spacer(Modifier.height(16.dp))
        SavingsRatePanel(months, axis, selected, onSelect)
    }

    GroupTitle(stringResource(R.string.trends_spending))
    if (detail.categories.isNotEmpty()) {
        CategoriesPanel(detail.categories, axis, selected, onSelect)
        Spacer(Modifier.height(16.dp))
    }
    if (detail.changes.isNotEmpty()) {
        ChangesPanel(detail.changes, currency)
        Spacer(Modifier.height(16.dp))
    }
    if (detail.shifts.isNotEmpty()) {
        ShiftsPanel(detail.shifts, currency)
        Spacer(Modifier.height(16.dp))
    }
    FixedFlexiblePanel(months, axis, selected, phrase, onSelect)

    GroupTitle(stringResource(R.string.trends_habits))
    StatTiles(habitStats(months, detail.habits, currency, phrase))
    detail.punchCard?.let { cells ->
        Spacer(Modifier.height(16.dp))
        WhenPanel(cells, currency)
    }
    detail.calendar?.let { calendar ->
        Spacer(Modifier.height(16.dp))
        CalendarPanel(calendar, currency)
    }

    val records = recordStats(detail.records, currency)
    if (records.isNotEmpty()) {
        GroupTitle(stringResource(R.string.trends_records))
        StatTiles(records)
    }
}

/** The net worth now, and how it moved over the range: of how much, when it started above nothing. */
@Composable
internal fun NetWorthSummary(detail: TrendsDetail, range: TrendsRange, currency: String) {
    val months = detail.months
    val first = months.first().netWorth
    val change = months.last().netWorth - first
    val phrase = rangePhrase(range, months.first().period)
    val page = MaterialTheme.colorScheme.background
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = stringResource(R.string.net_worth),
        style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(4.dp))
    BalanceRow(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .testTag("trends_net_worth"),
        currency = currency,
        balance = months.last().netWorth,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = if (first > 0) {
            stringResource(
                R.string.trends_change,
                signed(change, currency),
                currency,
                phrase,
                signedPercent(change / first)
            )
        } else {
            stringResource(R.string.trends_change_amount, signed(change, currency), currency, phrase)
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = if (change >= 0) incomeGreen(page) else expenseRed(page),
            fontWeight = FontWeight.Bold,
        ),
    )
    detail.realNetWorth?.let { real ->
        // In today's euros, the range's first month is worth more than it was.
        val realChange = real.last() - real.first()
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = if (real.first() > 0) {
                stringResource(
                    R.string.trends_after_inflation,
                    signed(realChange, currency),
                    currency,
                    signedPercent(realChange / real.first())
                )
            } else {
                stringResource(R.string.trends_after_inflation_amount, signed(realChange, currency), currency)
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (realChange >= 0) incomeGreen(page) else expenseRed(page),
            ),
        )
    }
}

// ---- Parts

// Legend dots, a few to a row.
@Composable
internal fun LegendRows(items: List<Pair<Color, String>>) {
    items.chunked(LegendsInRow).forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            row.forEachIndexed { column, (color, label) ->
                if (column > 0) Spacer(Modifier.width(12.dp))
                LegendDot(color = color, label = label)
            }
        }
    }
}

/**
 * The labels under a chart of [periods], a slot each: short names for a few months, initials for
 * a year, and past that only each January's year, and the first month's unless a January is near.
 */
@Composable
@ReadOnlyComposable
internal fun monthAxis(periods: List<TimePeriod>): List<Pair<Float, String>> {
    val names = periods.map { monthName(it) }
    return when {
        periods.size <= ShortNamesUpTo -> evenly(names.map { it.take(ShortName) })
        periods.size <= InitialsUpTo -> evenly(names.map { it.take(1) })
        else -> {
            val januaries = periods.indices.filter { periods[it].month?.monthValue == 1 }
            val first = 0.takeIf { januaries.none { january -> january < LabelRoom } }
            (listOfNotNull(first) + januaries).map { index ->
                (index + Half) / periods.size to "${names[index].take(ShortName)} ${periods[index].year ?: ""}".trim()
            }
        }
    }
}

// The months after [last], as many as [count].
internal fun ahead(last: TimePeriod, count: Int): List<TimePeriod> {
    val month = last.month ?: return emptyList()
    val year = last.year ?: return emptyList()
    return (1..count).map { month.incrementMonthPeriod(it.toLong(), year) }
}

@Composable
@ReadOnlyComposable
private fun rangePhrase(range: TrendsRange, first: TimePeriod): String = when (range) {
    TrendsRange.ThreeMonths -> stringResource(R.string.trends_in_three_months)
    TrendsRange.SixMonths -> stringResource(R.string.trends_in_six_months)
    TrendsRange.Year -> stringResource(R.string.trends_in_a_year)
    TrendsRange.All -> stringResource(R.string.trends_since, monthYear(first))
}

@Composable
@ReadOnlyComposable
internal fun overTitle(range: TrendsRange, first: TimePeriod): String = when (range) {
    TrendsRange.ThreeMonths -> stringResource(R.string.trends_over_three_months)
    TrendsRange.SixMonths -> stringResource(R.string.trends_over_six_months)
    TrendsRange.Year -> stringResource(R.string.trends_over_the_year)
    TrendsRange.All -> stringResource(R.string.trends_over_since, monthYear(first))
}

@Composable
@ReadOnlyComposable
internal fun monthName(period: TimePeriod): String = period.month?.displayName().orEmpty()

@Composable
@ReadOnlyComposable
internal fun monthYear(period: TimePeriod): String =
    stringResource(R.string.trends_month_year, monthName(period), period.year ?: 0)

@Composable
@ReadOnlyComposable
internal fun weekdayDate(day: LocalDate): String =
    stringResource(
        R.string.trends_weekday_date,
        weekdayName(day.dayOfWeek, TextStyle.FULL),
        dayLabel(day)
    )

@Composable
@ReadOnlyComposable
internal fun categoryName(series: CategorySeries): String = when {
    series.other -> stringResource(R.string.trends_others)
    else -> series.category?.name?.value ?: stringResource(R.string.unspecified)
}

// A stacked part's color: its own (its grey in minimal colors), and grey for the others together:
// a lighter one in minimal colors, where the items' greys are near the text's.
internal fun partColor(color: Color?, other: Boolean): Color = when {
    other || color == null -> if (ColorMode.minimal) Gray.copy(alpha = OthersAlpha) else Gray
    else -> color
}

// The months to average over: the full ones, or all of them when none is full yet.
internal fun fullOrAll(months: List<MonthTrend>): List<MonthTrend> = months.filter { it.full }.ifEmpty { months }

internal fun signed(amount: Double, currency: String) = (if (amount >= 0) "+" else "-") + abs(amount).format(currency)

internal fun signedPercent(share: Double) = (if (share >= 0) "+" else "-") + percent(abs(share))

internal fun shadeAlpha(level: Int) = ShadeBase + ShadeStep * level

internal const val DaysInWeek = 7
private const val ShortNamesUpTo = 12
private const val InitialsUpTo = 15
private const val ShortName = 3
private const val Half = 0.5f

// The slots a label needs past the first month's before the next one.
private const val LabelRoom = 6
private const val LegendsInRow = 3
private const val ShadeBase = 0.2f
private const val ShadeStep = 0.2f
private const val OthersAlpha = 0.45f

internal val BarsHeight = 130.dp
