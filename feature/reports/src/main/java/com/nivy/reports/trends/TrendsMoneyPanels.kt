package com.nivy.reports.trends

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.toComposeColor
import com.nivy.ui.R
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.ui.component.AxisLabels
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.approxAmount
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.utils.amountAndCode
import com.nivy.wallet.utils.format
import java.time.YearMonth

// ---- Net worth

@Composable
internal fun NetWorthPanel(
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
                add(amountAndCode(chosen.netWorth, currency) to BubbleLine.Amount)
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
                    stringResource(
                        R.string.trends_likely_end,
                        monthName(ahead.last()),
                        approxAmount(end.likely),
                        currency,
                    )
                )
                Spacer(Modifier.height(2.dp))
                SmallNote(
                    stringResource(R.string.month_likely_range, approxAmount(end.low), approxAmount(end.high), currency)
                )
            }
            outlook.milestone?.let { milestone ->
                Spacer(Modifier.height(12.dp))
                PanelNote(
                    stringResource(
                        R.string.trends_likely_milestone,
                        approxAmount(milestone.amount),
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
internal fun AccountsPanel(
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
internal fun IncomeExpensesPanel(
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
internal fun SavedPanel(
    months: List<MonthTrend>,
    axis: List<Pair<Float, String>>,
    selected: Int,
    currency: String,
    onSelect: (Int) -> Unit,
) {
    val average = fullOrAll(months).map { it.saved }.average()
    ChartPanel(
        title = stringResource(R.string.trends_saved_each_month),
        legend = { LegendText(stringResource(R.string.month_average_short, amountAndCode(average, currency))) },
    ) {
        SavedBars(months.map { it.saved }, average, selected, onSelect)
        AxisLabels(axis, selectedIndex = selected.takeIf { axis.size == months.size })
    }
}

@Composable
internal fun SavingsRatePanel(
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

private const val ForecastLegendAlpha = 0.45f
private const val LikelyLegendAlpha = 0.35f
