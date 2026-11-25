package com.nivy.transactions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.ui.R
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.component.AxisLabels
import com.nivy.wallet.ui.component.ChartLineWidth
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.ChartPointRing
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.dayLabel
import com.nivy.wallet.ui.component.pointInRing
import com.nivy.wallet.utils.format
import java.time.LocalDate
import kotlin.math.abs

/**
 * The period's money added up, under its switcher, its title folding it: on an account's page what
 * came in and what went out, each line's end with what it came to; on a category's, its one line
 * over the period before's, as the net worth screen's pace.
 */
@Composable
internal fun PeriodChartPanel(
    chart: PeriodChart,
    period: TimePeriod,
    currency: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val green = incomeGreen(panel)
    val red = expenseRed(panel)
    val income = chart.income
    val expenses = chart.expenses
    if (income != null && expenses != null) {
        ChartPanel(
            modifier = modifier.testTag("period_chart"),
            title = stringResource(R.string.income_vs_expenses),
            legend = {
                LegendDot(color = green, label = stringResource(R.string.income))
                Spacer(Modifier.width(12.dp))
                LegendDot(color = red, label = stringResource(R.string.expenses))
            },
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        ) {
            AddedUpLines(chart, lines = listOf(income to green, expenses to red), currency = currency)
            AxisLabels(chartAxis(chart))
            Spacer(Modifier.height(12.dp))
            val left = income.last() - expenses.last()
            PanelNote(
                text = stringResource(
                    if (left >= 0) R.string.period_chart_more_in else R.string.period_chart_more_out,
                    dayLabel(chart.through),
                    abs(left).format(currency),
                    currency,
                ),
                color = Gray,
            )
        }
    } else {
        val line = income ?: expenses ?: return
        val color = if (income != null) green else red
        val before = chart.before
        val names = periodNames(period, chart)
        ChartPanel(
            modifier = modifier.testTag("period_chart"),
            title = when {
                before != null -> stringResource(R.string.month_compared_with, names.beforeName)
                income != null -> stringResource(R.string.income)
                else -> stringResource(R.string.expenses)
            },
            legend = {
                if (before != null) {
                    LegendDot(color = color, label = names.now)
                    Spacer(Modifier.width(12.dp))
                    LegendDot(color = Gray, label = names.before)
                }
            },
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        ) {
            AddedUpLines(chart, lines = listOf(line to color), currency = null)
            AxisLabels(chartAxis(chart))
            if (before != null) {
                Spacer(Modifier.height(12.dp))
                // Less than before is good for spending, more is for income.
                val less = before.bySameDay - line.last()
                PanelNote(
                    text = stringResource(
                        if (less >= 0) R.string.month_less_than_by else R.string.month_more_than_by,
                        abs(less).format(currency),
                        currency,
                        dayLabel(before.sameDay),
                    ),
                    color = if ((less >= 0) == (income == null)) green else red,
                )
            }
        }
    }
}

/**
 * The [lines], each in its color, to the day the period got to, ending in a ringed point; the period
 * before's, grey and thinner under them, a smaller point where it was by then. With a [currency],
 * each end is labeled with what it came to, the highest over it, the others under theirs.
 */
@Composable
private fun AddedUpLines(chart: PeriodChart, lines: List<Pair<List<Double>, Color>>, currency: String?) {
    val ring = MaterialTheme.colorScheme.surfaceVariant
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
    )
    val ends = currency?.let { lines.map { (values, _) -> measurer.measure(values.last().format(currency), style) } }
    val before = chart.before
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (ends != null) LabeledHeight else Height)
            .testTag("period_chart_lines")
    ) {
        val count = maxOf(chart.points.size, before?.points?.size ?: 0)
        if (count < 2) return@Canvas
        val highest = (lines.map { it.first.last() } + listOfNotNull(before?.addedUp?.last())).max()
        val max = highest.takeIf { it > 0 }?.times(Headroom) ?: 1.0
        val top = if (ends != null) LabelSpace.toPx() else ChartPointRing.toPx()
        val bottom = size.height - ChartPointRing.toPx()
        fun x(index: Int) = index / (count - 1f) * size.width
        fun y(value: Double) = bottom - (value / max).toFloat() * (bottom - top)
        fun path(values: List<Double>) = Path().apply {
            moveTo(x(0), y(values.first()))
            for (index in 1..values.lastIndex) lineTo(x(index), y(values[index]))
        }
        if (before != null) drawPath(path(before.addedUp), Gray, style = lineStroke(BeforeWidth))
        lines.forEach { (values, color) -> drawPath(path(values), color, style = lineStroke(ChartLineWidth)) }
        val reached = lines.first().first.lastIndex
        before?.addedUp?.getOrNull(reached)?.let {
            pointInRing(Offset(x(reached), y(it)), Gray, ring, SmallPoint, SmallRing)
        }
        val highestLine = lines.indices.maxBy { lines[it].first.last() }
        lines.forEachIndexed { index, (values, color) ->
            val end = Offset(x(values.lastIndex), y(values.last()))
            pointInRing(end, color, ring)
            ends?.get(index)?.let { if (index == highestLine) labelAbove(it, end) else labelBelow(it, end) }
        }
    }
}

// Under the lines: the period's first point, the one it got to unless it's near either end, and its last.
@Composable
@ReadOnlyComposable
private fun chartAxis(chart: PeriodChart): List<Pair<Float, String>> {
    val count = maxOf(chart.points.size, chart.before?.points?.size ?: 0)
    if (count < 2) return emptyList()
    val reached = chart.points.indexOfLast { it <= chart.through }
    val at = reached / (count - 1f)
    return listOfNotNull(
        0f to pointLabel(chart.points.first(), chart),
        (at to pointLabel(chart.points[reached], chart)).takeIf {
            reached < chart.points.lastIndex && at > EdgeRoom && at < 1 - EdgeRoom
        },
        chart.points.lastIndex / (count - 1f) to pointLabel(chart.points.last(), chart),
    )
}

// A point's day: its number in a calendar month, else its date; its month, with the year when the
// period spans more than one; or its year.
@Composable
@ReadOnlyComposable
private fun pointLabel(day: LocalDate, chart: PeriodChart): String {
    val first = chart.points.first()
    val last = chart.points.last()
    return when (chart.step) {
        ChartStep.Day -> if (first.dayOfMonth == 1 && first.month == last.month) "${day.dayOfMonth}" else dayLabel(day)
        ChartStep.Month -> {
            val name = Month.fromMonthValue(day.monthValue).displayName().take(ShortMonth)
            if (first.year == last.year) name else stringResource(R.string.trends_month_year, name, day.year)
        }
        ChartStep.Year -> "${day.year}"
    }
}

/** The period's name and the period before's, short for the legend, and the latter whole for the title. */
private class PeriodNames(val now: String, val before: String, val beforeName: String)

// A month's are month names; a calendar year's, years; any other period's, now and before.
@Composable
@ReadOnlyComposable
private fun periodNames(period: TimePeriod, chart: PeriodChart): PeriodNames {
    val month = period.month
    val first = chart.points.first()
    return when {
        month != null -> {
            val previous = Month.fromMonthValue((month.monthValue + MonthsInYear - 2) % MonthsInYear + 1)
            PeriodNames(
                now = month.displayName().take(ShortMonth),
                before = previous.displayName().take(ShortMonth),
                beforeName = previous.displayName(),
            )
        }

        chart.step == ChartStep.Month && chart.points.size == MonthsInYear && first.dayOfYear == 1 ->
            PeriodNames(now = "${first.year}", before = "${first.year - 1}", beforeName = "${first.year - 1}")

        else -> PeriodNames(
            now = stringResource(R.string.period_chart_now),
            before = stringResource(R.string.period_chart_before),
            beforeName = stringResource(R.string.period_chart_period_before),
        )
    }
}

// A label centered over a point, kept inside the chart.
private fun DrawScope.labelAbove(text: TextLayoutResult, point: Offset) {
    drawText(text, topLeft = Offset(centered(text, point), point.y - text.size.height - LabelGap.toPx()))
}

// A label centered under a point, kept inside the chart.
private fun DrawScope.labelBelow(text: TextLayoutResult, point: Offset) {
    drawText(text, topLeft = Offset(centered(text, point), point.y + LabelBelowGap.toPx()))
}

private fun DrawScope.lineStroke(width: Dp) = Stroke(width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.centered(text: TextLayoutResult, point: Offset): Float =
    (point.x - text.size.width / 2f).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f))

private const val Headroom = 1.1
private const val EdgeRoom = 0.12f
private const val ShortMonth = 3
private const val MonthsInYear = 12
private val Height = 110.dp
private val LabeledHeight = 136.dp
private val LabelSpace = 28.dp
private val LabelGap = 8.dp
private val LabelBelowGap = 10.dp
private val BeforeWidth = 2.5.dp
private val SmallPoint = 3.5.dp
private val SmallRing = 6.dp
