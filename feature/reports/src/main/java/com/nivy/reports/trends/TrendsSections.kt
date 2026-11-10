package com.nivy.reports.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Category
import com.nivy.design.system.colors.Blue
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.minimalTone
import com.nivy.design.system.colors.toComposeColor
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.component.AxisLabels
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.GroupTitle
import com.nivy.wallet.ui.component.InfoRow
import com.nivy.wallet.ui.component.InfoRows
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.Stat
import com.nivy.wallet.ui.component.StatTiles
import com.nivy.wallet.ui.component.evenly
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.domain.pure.forecast.Shift
import com.nivy.wallet.utils.format
import java.time.DayOfWeek
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

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
}

// ---- Net worth

@Composable
private fun NetWorthPanel(
    detail: TrendsDetail,
    range: TrendsRange,
    selected: Int,
    currency: String,
    onSelect: (Int) -> Unit,
) {
    val months = detail.months
    val chosen = months[selected]
    val before = months.getOrNull(selected - 1)
    val outlook = detail.outlook
    ChartPanel(
        title = overTitle(range, months.first().period),
        legend = {
            LegendDot(color = Nivy, label = stringResource(R.string.trends_actual))
            if (outlook != null) {
                Spacer(Modifier.width(12.dp))
                LegendDot(color = Nivy.copy(alpha = LikelyLegendAlpha), label = stringResource(R.string.trends_likely))
            } else if (detail.forecast.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                LegendDot(
                    color = Nivy.copy(alpha = ForecastLegendAlpha),
                    label = stringResource(R.string.trends_forecast)
                )
            }
        },
    ) {
        NetWorthChart(
            values = months.map { it.netWorth },
            forecast = detail.forecast,
            selected = selected,
            bubble = buildList {
                add(monthYear(chosen.period) to BubbleLine.Label)
                add(money(chosen.netWorth, currency) to BubbleLine.Amount)
                if (before != null) {
                    val moved = chosen.netWorth - before.netWorth
                    val change = stringResource(
                        R.string.trends_in_month,
                        signed(moved, currency),
                        currency,
                        monthName(chosen.period),
                    )
                    add(change to if (moved >= 0) BubbleLine.Gain else BubbleLine.Loss)
                }
            },
            onSelect = onSelect,
            outlook = outlook?.months,
        )
        val ahead = ahead(months.last().period, outlook?.months?.size ?: detail.forecast.size)
        val periods = months.map { it.period } + ahead
        val axis = monthAxis(periods)
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == periods.size })
        if (outlook != null) {
            val end = outlook.months.lastOrNull()
            if (end != null && ahead.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                PanelNote(
                    stringResource(R.string.trends_likely_end, monthName(ahead.last()), approx(end.likely), currency)
                )
                Spacer(Modifier.height(2.dp))
                SmallNote(stringResource(R.string.month_likely_range, approx(end.low), approx(end.high), currency))
            }
            outlook.milestone?.let { milestone ->
                Spacer(Modifier.height(12.dp))
                PanelNote(
                    stringResource(
                        R.string.trends_likely_milestone,
                        approx(milestone.amount),
                        currency,
                        yearMonthName(milestone.likely),
                    )
                )
                val late = milestone.late
                val range = when {
                    late == null -> stringResource(R.string.trends_likely_or_later, yearMonthName(milestone.early))
                    late == milestone.early -> null
                    else -> stringResource(
                        R.string.trends_likely_between,
                        if (milestone.early.year == late.year) {
                            Month.fromMonthValue(milestone.early.monthValue).displayName()
                        } else {
                            yearMonthName(milestone.early)
                        },
                        yearMonthName(late),
                    )
                }
                range?.let {
                    Spacer(Modifier.height(2.dp))
                    SmallNote(it)
                }
            }
        } else {
            detail.milestone?.let { milestone ->
                Spacer(Modifier.height(12.dp))
                PanelNote(
                    text = stringResource(
                        R.string.trends_milestone,
                        milestone.amount.format(currency),
                        currency,
                        yearMonthName(milestone.by),
                    ),
                    color = Gray,
                )
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun yearMonthName(month: YearMonth): String =
    stringResource(R.string.trends_month_year, Month.fromMonthValue(month.monthValue).displayName(), month.year)

/** A small grey line under a panel's note. */
@Composable
private fun SmallNote(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(color = Gray),
    )
}

@Composable
private fun AccountsPanel(
    accounts: List<AccountSeries>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = accounts.map { series ->
        partColor(series.account?.color?.value?.toComposeColor(), other = series.account == null)
    }
    ChartPanel(title = stringResource(R.string.trends_by_account), legend = {}) {
        StackedArea(accounts.map { it.values }, colors, selected, onSelect)
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == accounts.first().values.size })
        Spacer(Modifier.height(12.dp))
        LegendRows(
            accounts.mapIndexed { index, series ->
                colors[index] to (series.account?.name?.value ?: stringResource(R.string.trends_others))
            }
        )
    }
}

// ---- Money in and out

@Composable
private fun IncomeExpensesPanel(
    months: List<MonthTrend>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    currency: String,
    onSelect: (Int) -> Unit,
) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        title = stringResource(R.string.income_vs_expenses),
        legend = {
            LegendDot(color = incomeGreen(panel), label = stringResource(R.string.income))
            Spacer(Modifier.width(12.dp))
            LegendDot(color = expenseRed(panel), label = stringResource(R.string.expenses))
        },
    ) {
        PairedBars(months.map { it.income }, months.map { it.expense }, selected, onSelect)
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == months.size })
        Spacer(Modifier.height(12.dp))
        val chosen = months[selected]
        PanelNote(
            text = stringResource(
                R.string.trends_month_in_out,
                monthName(chosen.period),
                chosen.income.format(currency),
                chosen.expense.format(currency),
                currency,
            ),
            color = Gray,
        )
    }
}

@Composable
private fun SavedPanel(
    months: List<MonthTrend>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    currency: String,
    onSelect: (Int) -> Unit,
) {
    val average = fullOrAll(months).map { it.saved }.average()
    ChartPanel(
        title = stringResource(R.string.trends_saved_each_month),
        legend = { LegendText(stringResource(R.string.month_average_short, money(average, currency))) },
    ) {
        SavedBars(months.map { it.saved }, average, selected, onSelect)
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == months.size })
    }
}

@Composable
private fun SavingsRatePanel(
    months: List<MonthTrend>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val average = fullOrAll(months).mapNotNull { it.savingsRate }.takeIf { it.isNotEmpty() }?.average()
    ChartPanel(
        title = stringResource(R.string.trends_savings_rate),
        legend = { average?.let { LegendText(stringResource(R.string.month_average_short, percent(it))) } },
    ) {
        RateLine(months.map { it.savingsRate }, average, selected, onSelect)
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == months.size })
    }
}

// ---- Spending

@Composable
private fun CategoriesPanel(
    categories: List<CategorySeries>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = categories.map { part -> partColor(categoryColor(part.category), part.other) }
    ChartPanel(title = stringResource(R.string.trends_where_it_goes), legend = {}) {
        StackedBars(
            series = categories.map { it.values },
            colors = colors,
            selected = selected,
            height = CategoryBarsHeight,
            tag = "trends_categories_chart",
            onSelect = onSelect,
        )
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == categories.first().values.size })
        Spacer(Modifier.height(12.dp))
        LegendRows(categories.mapIndexed { index, series -> colors[index] to categoryName(series) })
    }
}

@Composable
private fun ChangesPanel(changes: List<CategoryShift>, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        title = stringResource(R.string.trends_biggest_changes),
        legend = { LegendText(stringResource(R.string.trends_last_three_months)) },
    ) {
        InfoRows(changes) { shift ->
            val change = shift.change
            InfoRow(
                dot = categoryColor(shift.category),
                title = shift.category?.name?.value ?: stringResource(R.string.unspecified),
                note = if (shift.before > 0) {
                    stringResource(R.string.trends_from, shift.before.format(currency), currency)
                } else {
                    stringResource(R.string.trends_a_month_plain)
                },
                value = money(shift.average, currency),
                trailing = when {
                    change == null -> stringResource(R.string.month_new)
                    abs(change) < SameChange -> stringResource(R.string.month_same)
                    else -> signedPercent(change)
                },
                // Spending more is red, less is green.
                trailingColor = when {
                    change == null || abs(change) < SameChange -> Gray
                    change > 0 -> expenseRed(panel)
                    else -> incomeGreen(panel)
                },
            )
        }
    }
}

/**
 * The categories whose spending shifted over the range's full months: to a new level from a
 * month on, or steadily, each over its months, the level it moved from and the one it's at.
 */
@Composable
private fun ShiftsPanel(shifts: List<HabitShift>, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        modifier = Modifier.testTag("trends_habit_shifts"),
        title = stringResource(R.string.trends_habit_shifts),
        legend = { LegendText(stringResource(R.string.trends_full_months)) },
    ) {
        Column {
            shifts.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(20.dp))
                val shift = item.shift
                val up = shift.after > shift.before
                val color = categoryColor(item.category)
                ShiftRow(
                    dot = color,
                    title = item.category?.name?.value ?: stringResource(R.string.unspecified),
                    note = when (shift) {
                        is Shift.Step -> stringResource(
                            if (up) R.string.trends_up_since else R.string.trends_down_since,
                            monthName(item.periods[shift.at]),
                            approx(shift.before),
                            approx(shift.after),
                            currency,
                        )
                        is Shift.Drift -> stringResource(
                            if (up) R.string.trends_rising else R.string.trends_falling,
                            approx(abs(shift.perMonth)),
                            currency,
                        )
                    },
                    value = "${approx(shift.after)} $currency",
                    trailing = signedPercent((shift.after - shift.before) / shift.before),
                    // Spending more is red, less is green.
                    trailingColor = if (up) expenseRed(panel) else incomeGreen(panel),
                )
                Spacer(Modifier.height(8.dp))
                ShiftChart(item.values, shift, color)
            }
        }
    }
}

/** A shift's row: a [dot], a [title] over a [note] of up to two lines, a [value] and a [trailing] change. */
@Composable
private fun ShiftRow(dot: Color, title: String, note: String, value: String, trailing: String, trailingColor: Color) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(dot, MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = note,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(color = Gray),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        )
        Text(
            modifier = Modifier.width(56.dp),
            text = trailing,
            maxLines = 1,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium.copy(color = trailingColor, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * A shift's months as small bars in the category's color, and over them the levels it held: the
 * one before and the one after a step, or the line a steady drift follows.
 */
@Composable
private fun ShiftChart(values: List<Double>, shift: Shift, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 8.dp)
            .height(ShiftChartHeight)
    ) {
        if (values.isEmpty()) return@Canvas
        val max = maxOf(values.max(), shift.before, shift.after).takeIf { it > 0 }?.times(ShiftHeadroom) ?: 1.0
        val slot = size.width / values.size
        val bar = slot * ShiftBarFraction
        fun y(value: Double) = size.height - (value / max).toFloat() * size.height
        values.forEachIndexed { index, value ->
            val top = y(value)
            drawRoundRect(
                color.copy(alpha = ShiftBarAlpha),
                topLeft = Offset(index * slot + (slot - bar) / 2, top),
                size = Size(bar, size.height - top),
                cornerRadius = CornerRadius(ShiftBarRadius.toPx()),
            )
        }
        val stroke = ShiftLineWidth.toPx()
        when (shift) {
            is Shift.Step -> {
                val at = shift.at * slot
                drawLine(color, Offset(0f, y(shift.before)), Offset(at, y(shift.before)), stroke, StrokeCap.Round)
                drawLine(
                    color,
                    Offset(at, y(shift.before)),
                    Offset(at, y(shift.after)),
                    stroke / 2,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(ShiftDash.toPx(), ShiftDash.toPx())),
                )
                drawLine(color, Offset(at, y(shift.after)), Offset(size.width, y(shift.after)), stroke, StrokeCap.Round)
            }
            is Shift.Drift -> drawLine(
                color,
                Offset(slot / 2, y(shift.before)),
                Offset(size.width - slot / 2, y(shift.after)),
                stroke,
                StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun FixedFlexiblePanel(
    months: List<MonthTrend>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    phrase: String,
    onSelect: (Int) -> Unit,
) {
    // Not accounts nor categories: in minimal colors, greys by their place.
    val fixedColor = if (ColorMode.minimal) minimalTone(0) else Nivy
    val flexibleColor = if (ColorMode.minimal) minimalTone(1) else Blue
    ChartPanel(
        title = stringResource(R.string.month_fixed_and_flexible),
        legend = {
            LegendDot(color = fixedColor, label = stringResource(R.string.month_fixed))
            Spacer(Modifier.width(12.dp))
            LegendDot(color = flexibleColor, label = stringResource(R.string.month_flexible))
        },
    ) {
        StackedBars(
            series = listOf(months.map { it.fixed }, months.map { it.expense - it.fixed }),
            colors = listOf(fixedColor, flexibleColor),
            selected = selected,
            height = BarsHeight,
            tag = "trends_fixed_chart",
            onSelect = onSelect,
        )
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == months.size })
        trendChange(months.filter { it.full }.map { it.fixed })?.let { change ->
            Spacer(Modifier.height(12.dp))
            PanelNote(
                text = when {
                    abs(change) < SameChange -> stringResource(R.string.trends_fixed_steady, phrase)
                    change > 0 -> stringResource(R.string.trends_fixed_grew, percent(change), phrase)
                    else -> stringResource(R.string.trends_fixed_fell, percent(-change), phrase)
                },
                color = Gray,
            )
        }
    }
}

// ---- Habits

@Composable
@ReadOnlyComposable
private fun habitStats(months: List<MonthTrend>, habits: RangeHabits, currency: String, phrase: String): List<Stat> {
    val full = months.filter { it.full }
    return buildList {
        if (full.isNotEmpty()) {
            val purchases = full.map { it.habits.purchases.toDouble() }
            add(
                Stat(
                    label = stringResource(R.string.month_purchases),
                    value = stringResource(R.string.trends_a_month, purchases.average().roundToInt().toString()),
                    note = changeNote(trendChange(purchases), phrase),
                    trend = purchases,
                )
            )
        }
        habits.typical?.let { typical ->
            val typicals = full.mapNotNull { it.habits.typical }
            add(
                Stat(
                    label = stringResource(R.string.month_typical_purchase),
                    value = money(typical, currency),
                    note = changeNote(trendChange(typicals), phrase),
                    trend = typicals,
                )
            )
        }
        if (full.isNotEmpty()) add(noSpendStat(full))
        addAll(shareStats(full, habits, currency))
    }
}

@Composable
@ReadOnlyComposable
private fun noSpendStat(full: List<MonthTrend>): Stat {
    val days = full.map { it.habits.noSpendDays.toDouble() }
    val best = full.maxBy { it.habits.noSpendDays }
    return Stat(
        label = stringResource(R.string.month_no_spend_days),
        value = stringResource(R.string.trends_a_month, days.average().roundToInt().toString()),
        note = stringResource(R.string.trends_best_month, best.habits.noSpendDays, monthName(best.period)),
        trend = days,
    )
}

// The shares of the spending on weekends, in small purchases and in the evening.
@Composable
@ReadOnlyComposable
private fun shareStats(full: List<MonthTrend>, habits: RangeHabits, currency: String): List<Stat> = buildList {
    habits.weekendShare?.let { share ->
        add(
            Stat(
                label = stringResource(R.string.month_weekends),
                value = stringResource(R.string.month_of_spending, percent(share)),
                note = stringResource(R.string.trends_on_days, percent(habits.weekendDays)),
                trend = full.mapNotNull { month -> month.habits.weekend.shareOf(month.habits.spent) },
            )
        )
    }
    habits.smallShare?.let { share ->
        add(
            Stat(
                label = stringResource(R.string.month_small_purchases),
                value = stringResource(R.string.month_of_spending, percent(share)),
                note = stringResource(R.string.trends_under_each, compact(habits.smallUnder), currency),
                trend = full.mapNotNull { month -> month.habits.small.shareOf(month.habits.spent) },
            )
        )
    }
    habits.eveningShare?.let { share ->
        val evening = DayParts[EveningPart]
        add(
            Stat(
                label = stringResource(R.string.trends_evenings),
                value = stringResource(R.string.month_of_spending, percent(share)),
                note = stringResource(R.string.month_hours, evening.first, evening.last + 1),
                trend = full.mapNotNull { month -> month.habits.evening.shareOf(month.habits.timed) },
            )
        )
    }
}

private fun Double.shareOf(whole: Double): Double? = if (whole > 0) this / whole else null

@Composable
@ReadOnlyComposable
private fun changeNote(change: Double?, phrase: String): String =
    if (change == null) {
        stringResource(R.string.trends_on_average)
    } else {
        stringResource(R.string.trends_change_over, signedPercent(change), phrase)
    }

@Composable
private fun WhenPanel(cells: List<List<Double>>, currency: String) {
    val busiest = cells.indices.flatMap { row -> cells[row].indices.map { column -> row to column } }
        .maxBy { (row, column) -> cells[row][column] }
    val weekdays = DayOfWeek.entries
    val parts =
        listOf(
            R.string.trends_mornings,
            R.string.trends_afternoons,
            R.string.trends_evenings_part,
            R.string.trends_nights
        )
    ChartPanel(
        title = stringResource(R.string.month_when_you_spend),
        legend = { LegendText(stringResource(R.string.trends_on_average)) },
    ) {
        PunchCard(
            cells = cells,
            rows = weekdays.map { weekdayName(it, TextStyle.SHORT) },
            columns = listOf(
                R.string.month_morning,
                R.string.month_afternoon,
                R.string.month_evening,
                R.string.month_night
            )
                .map { stringResource(it) },
            strongRow = busiest.first,
        )
        Spacer(Modifier.height(12.dp))
        val (row, column) = busiest
        PanelNote(
            text = stringResource(
                R.string.trends_most_on,
                weekdayName(weekdays[row], TextStyle.FULL),
                stringResource(parts[column]),
                cells[row][column].format(currency),
                currency,
            ),
        )
    }
}

@Composable
private fun CalendarPanel(calendar: SpendingCalendar, currency: String) {
    val red = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
    val shades = listOf(MaterialTheme.colorScheme.surface) +
        (1..CalendarShades).map { red.copy(alpha = shadeAlpha(it)) }
    val weeks = (calendar.amounts.size + DaysInWeek - 1) / DaysInWeek
    ChartPanel(
        title = stringResource(R.string.trends_spending_calendar),
        legend = { LegendText(pluralStringResource(R.plurals.trends_last_weeks, weeks, weeks)) },
    ) {
        Box(Modifier.padding(horizontal = 8.dp)) { CalendarGrid(calendar, shades) }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.trends_less), style = MaterialTheme.typography.labelSmall.copy(color = Gray))
            Spacer(Modifier.width(6.dp))
            shades.forEach { shade ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .background(shade, MaterialTheme.shapes.extraSmall)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.trends_more), style = MaterialTheme.typography.labelSmall.copy(color = Gray))
        }
        calendar.top?.let { top ->
            Spacer(Modifier.height(12.dp))
            PanelNote(
                text = stringResource(
                    R.string.trends_most_spent_day,
                    weekdayDate(top.day),
                    top.amount.format(currency),
                    currency
                ),
            )
        }
    }
}

// ---- Records

@Composable
@ReadOnlyComposable
private fun recordStats(records: Records, currency: String): List<Stat> = buildList {
    records.mostSaved?.let {
        add(Stat(stringResource(R.string.trends_most_saved), "+${money(it.amount, currency)}", monthYear(it.period)))
    }
    records.mostSpent?.let {
        add(Stat(stringResource(R.string.trends_most_spent), money(it.amount, currency), monthYear(it.period)))
    }
    records.biggest?.let {
        val what = it.title ?: it.category?.name?.value ?: stringResource(R.string.unspecified)
        add(
            Stat(
                stringResource(R.string.trends_biggest_purchase),
                money(it.amount, currency),
                stringResource(R.string.trends_what_when, what, dateLabel(it.day)),
            )
        )
    }
    records.noSpendStreak?.let {
        add(
            Stat(
                stringResource(R.string.trends_no_spend_streak),
                pluralStringResource(R.plurals.trends_days, it.days, it.days),
                stringResource(R.string.trends_from_to, dateLabel(it.from), dateLabel(it.to)),
            )
        )
    }
    records.cheapestWeek?.let {
        add(
            Stat(
                stringResource(R.string.trends_cheapest_week),
                money(it.amount, currency),
                stringResource(
                    R.string.trends_from_to,
                    dateLabel(it.from),
                    dateLabel(it.from.plusDays(DaysInWeek - 1L))
                ),
            )
        )
    }
    records.busiestDay?.let {
        add(
            Stat(
                stringResource(R.string.month_busiest_day),
                pluralStringResource(R.plurals.month_purchases_count, it.purchases, it.purchases),
                weekdayDate(it.day),
            )
        )
    }
}

// ---- Parts

// Legend dots, a few to a row.
@Composable
private fun LegendRows(items: List<Pair<Color, String>>) {
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
private fun monthAxis(periods: List<TimePeriod>): List<Pair<Float, String>> {
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
private fun ahead(last: TimePeriod, count: Int): List<TimePeriod> {
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
private fun overTitle(range: TrendsRange, first: TimePeriod): String = when (range) {
    TrendsRange.ThreeMonths -> stringResource(R.string.trends_over_three_months)
    TrendsRange.SixMonths -> stringResource(R.string.trends_over_six_months)
    TrendsRange.Year -> stringResource(R.string.trends_over_the_year)
    TrendsRange.All -> stringResource(R.string.trends_over_since, monthYear(first))
}

@Composable
@ReadOnlyComposable
private fun monthName(period: TimePeriod): String = period.month?.displayName().orEmpty()

@Composable
@ReadOnlyComposable
private fun monthYear(period: TimePeriod): String =
    stringResource(R.string.trends_month_year, monthName(period), period.year ?: 0)

@Composable
@ReadOnlyComposable
private fun dateLabel(day: LocalDate): String = with(LocalTimeFormatter.current) {
    day.atStartOfDay().format(TimeFormatter.Style.DateOnly(includeWeekDay = false))
}

// A weekday's name in the app's language, read from the composition so a change of language redraws it.
@Composable
@ReadOnlyComposable
private fun weekdayName(day: DayOfWeek, style: TextStyle): String =
    day.getDisplayName(style, LocalConfiguration.current.locales[0])

@Composable
@ReadOnlyComposable
private fun weekdayDate(day: LocalDate): String =
    stringResource(
        R.string.trends_weekday_date,
        weekdayName(day.dayOfWeek, TextStyle.FULL),
        dateLabel(day)
    )

@Composable
@ReadOnlyComposable
private fun categoryName(series: CategorySeries): String = when {
    series.other -> stringResource(R.string.trends_others)
    else -> series.category?.name?.value ?: stringResource(R.string.unspecified)
}

// A category's own color, its grey in minimal colors; grey without a category.
private fun categoryColor(category: Category?): Color = category?.color?.value?.toComposeColor() ?: Gray

// A stacked part's color: its own (its grey in minimal colors), and grey for the others together:
// a lighter one in minimal colors, where the items' greys are near the text's.
private fun partColor(color: Color?, other: Boolean): Color = when {
    other || color == null -> if (ColorMode.minimal) Gray.copy(alpha = OthersAlpha) else Gray
    else -> color
}

// The months to average over: the full ones, or all of them when none is full yet.
private fun fullOrAll(months: List<MonthTrend>): List<MonthTrend> = months.filter { it.full }.ifEmpty { months }

private fun money(amount: Double, currency: String) = "${amount.format(currency)} $currency"

private fun signed(amount: Double, currency: String) = (if (amount >= 0) "+" else "-") + abs(amount).format(currency)

private fun signedPercent(share: Double) = (if (share >= 0) "+" else "-") + percent(abs(share))

/** A forecast's amount, as round as it deserves: to 10 from 100 on, whole below. */
private fun approx(amount: Double): String {
    val step = if (abs(amount) >= ApproxFrom) ApproxStep else 1.0
    return DecimalFormat("#,##0").format((amount / step).roundToInt() * step)
}

// An amount without its decimals when it's whole, as a size's edge.
private fun compact(amount: Double): String =
    if (amount == amount.roundToInt().toDouble()) "%,d".format(amount.roundToInt()) else "%,.2f".format(amount)

private fun shadeAlpha(level: Int) = ShadeBase + ShadeStep * level

private const val DaysInWeek = 7
private const val ShortNamesUpTo = 12
private const val InitialsUpTo = 15
private const val ShortName = 3
private const val Half = 0.5f

// The slots a label needs past the first month's before the next one.
private const val LabelRoom = 6
private const val LegendsInRow = 3
private const val CalendarShades = 4
private const val ShadeBase = 0.2f
private const val ShadeStep = 0.2f
private const val ForecastLegendAlpha = 0.45f
private const val LikelyLegendAlpha = 0.35f
private const val ApproxFrom = 100.0
private const val ApproxStep = 10.0
private const val ShiftHeadroom = 1.15
private const val ShiftBarFraction = 0.55f
private const val ShiftBarAlpha = 0.3f
private val ShiftChartHeight = 40.dp
private val ShiftBarRadius = 3.dp
private val ShiftLineWidth = 2.dp
private val ShiftDash = 3.dp
private const val OthersAlpha = 0.45f

// A change smaller than this is "same".
private const val SameChange = 0.005
private val CategoryBarsHeight = 150.dp
private val BarsHeight = 130.dp
