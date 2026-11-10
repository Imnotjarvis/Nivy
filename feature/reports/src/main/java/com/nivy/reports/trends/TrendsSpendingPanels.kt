package com.nivy.reports.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Blue
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.minimalTone
import com.nivy.ui.R
import com.nivy.wallet.domain.pure.forecast.Shift
import com.nivy.wallet.ui.component.AxisLabels
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.FindingRow
import com.nivy.wallet.ui.component.InfoRow
import com.nivy.wallet.ui.component.InfoRows
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.amountWithCode
import com.nivy.wallet.ui.component.approxAmount
import com.nivy.wallet.ui.component.categoryColor
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.utils.format
import kotlin.math.abs

// ---- Spending

@Composable
internal fun CategoriesPanel(
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
internal fun ChangesPanel(changes: List<CategoryShift>, currency: String) {
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
                value = amountWithCode(shift.average, currency),
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
internal fun ShiftsPanel(shifts: List<HabitShift>, currency: String) {
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
                FindingRow(
                    dot = color,
                    title = item.category?.name?.value ?: stringResource(R.string.unspecified),
                    note = when (shift) {
                        is Shift.Step -> stringResource(
                            if (up) R.string.trends_up_since else R.string.trends_down_since,
                            monthName(item.periods[shift.at]),
                            approxAmount(shift.before),
                            approxAmount(shift.after),
                            currency,
                        )
                        is Shift.Drift -> stringResource(
                            if (up) R.string.trends_rising else R.string.trends_falling,
                            approxAmount(abs(shift.perMonth)),
                            currency,
                        )
                    },
                    value = "${approxAmount(shift.after)} $currency",
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
internal fun FixedFlexiblePanel(
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

private const val ShiftHeadroom = 1.15
private const val ShiftBarFraction = 0.55f
private const val ShiftBarAlpha = 0.3f
private val ShiftChartHeight = 40.dp
private val ShiftBarRadius = 3.dp
private val ShiftLineWidth = 2.dp
private val ShiftDash = 3.dp

// A change smaller than this is "same".
private const val SameChange = 0.005
private val CategoryBarsHeight = 150.dp
