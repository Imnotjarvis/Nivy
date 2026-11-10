package com.nivy.reports.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.wallet.domain.pure.forecast.Band
import com.nivy.wallet.ui.component.percent
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The slot under a tap, or a drag across, a chart of [count] equal slots goes to [onSelect]. The
 * gestures keep the first [onSelect] for as long as [count] holds, so it should read the latest
 * callback, as one from rememberUpdatedState does: a new one would end a drag.
 */
private fun Modifier.monthSlots(count: Int, onSelect: (Int) -> Unit): Modifier {
    fun slot(x: Float, width: Int) = (x / width * count).toInt().coerceIn(0, count - 1)
    return this
        .pointerInput(count) { detectTapGestures { onSelect(slot(it.x, size.width)) } }
        .pointerInput(count) {
            detectHorizontalDragGestures(onDragStart = { onSelect(slot(it.x, size.width)) }) { change, _ ->
                onSelect(slot(change.position.x, size.width))
            }
        }
}

// The chosen month's slot, in the panel's lighter or darker shade.
private fun DrawScope.slot(index: Int, count: Int, color: Color) {
    val width = size.width / count
    drawRoundRect(
        color,
        topLeft = Offset(index * width, 0f),
        size = Size(width, size.height),
        cornerRadius = CornerRadius(SlotRadius.toPx()),
    )
}

/**
 * The net worth month by month and, dashed, the months [forecast] at the same pace; or, with an
 * [outlook], the months ahead as likely, in a band that 8 runs in 10 stay within. Over the chosen
 * month, a bubble tells its net worth and how it moved in that month: [bubble] gives its lines.
 */
@Composable
internal fun NetWorthChart(
    values: List<Double>,
    forecast: List<Double>,
    selected: Int,
    bubble: List<Pair<String, BubbleLine>>,
    onSelect: (Int) -> Unit,
    outlook: List<Band>? = null,
) {
    val line = Nivy
    val highlight = MaterialTheme.colorScheme.surface
    val ring = MaterialTheme.colorScheme.surfaceVariant
    val measurer = rememberTextMeasurer()
    val styles = mapOf(
        BubbleLine.Label to MaterialTheme.typography.labelSmall.copy(color = Gray),
        BubbleLine.Amount to MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
        ),
        BubbleLine.Gain to MaterialTheme.typography.labelSmall.copy(
            color = incomeGreen(highlight),
            fontWeight = FontWeight.Bold,
        ),
        BubbleLine.Loss to MaterialTheme.typography.labelSmall.copy(
            color = expenseRed(highlight),
            fontWeight = FontWeight.Bold,
        ),
    )
    val texts = bubble.map { (text, kind) -> measurer.measure(text, styles.getValue(kind)) }
    val count = values.size + (outlook?.size ?: forecast.size)
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(NetWorthHeight)
            .testTag("trends_net_worth_chart")
            .monthSlots(count) { select(it.coerceAtMost(values.lastIndex)) }
    ) {
        if (values.isEmpty()) return@Canvas
        val padding = BubblePadding.toPx()
        val bubbleHeight = texts.sumOf { it.size.height } + 2 * padding
        val top = bubbleHeight + BubbleGap.toPx()
        val bottom = size.height - PointRing.toPx()
        val all = values + (outlook?.flatMap { listOf(it.low, it.high) } ?: forecast)
        val margin = (all.max() - all.min()).coerceAtLeast(1.0) * ChartMargin
        val min = all.min() - margin
        val max = all.max() + margin
        val width = size.width / count
        fun x(index: Int) = index * width + width / 2
        fun y(value: Double) = top + ((max - value) / (max - min)).toFloat() * (bottom - top)

        slot(selected, count, highlight)
        val actual = Path().apply {
            moveTo(x(0), y(values[0]))
            for (index in 1..values.lastIndex) lineTo(x(index), y(values[index]))
        }
        if (values.size > 1) {
            val area = Path().apply {
                addPath(actual)
                lineTo(x(values.lastIndex), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = AreaAlpha), Color.Transparent), top, bottom))
            drawPath(actual, line, style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (outlook != null && outlook.isNotEmpty()) {
            val last = values.lastIndex
            val band = Path().apply {
                moveTo(x(last), y(values.last()))
                outlook.forEachIndexed { index, month -> lineTo(x(values.size + index), y(month.high)) }
                for (index in outlook.indices.reversed()) lineTo(x(values.size + index), y(outlook[index].low))
                close()
            }
            drawPath(band, line.copy(alpha = FanAlpha))
            val likely = Path().apply {
                moveTo(x(last), y(values.last()))
                outlook.forEachIndexed { index, month -> lineTo(x(values.size + index), y(month.likely)) }
            }
            drawPath(
                likely,
                line,
                style = Stroke(
                    LikelyWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(DotLength, LikelyGap.toPx())),
                ),
            )
            drawCircle(line, SmallPoint.toPx(), Offset(x(values.size + outlook.lastIndex), y(outlook.last().likely)))
        } else if (forecast.isNotEmpty()) {
            val ahead = Path().apply {
                moveTo(x(values.lastIndex), y(values.last()))
                forecast.forEachIndexed { index, value -> lineTo(x(values.size + index), y(value)) }
            }
            drawPath(
                ahead,
                line.copy(alpha = ForecastAlpha),
                style = Stroke(
                    LineWidth.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dash.toPx(), Dash.toPx())),
                ),
            )
        }
        values.forEachIndexed { index, value ->
            if (index != selected) drawCircle(line, SmallPoint.toPx(), Offset(x(index), y(value)))
        }
        val point = Offset(x(selected), y(values[selected]))
        drawLine(
            line.copy(alpha = GuideAlpha),
            Offset(point.x, top - padding),
            Offset(point.x, bottom),
            GuideWidth.toPx()
        )
        drawCircle(ring, PointRing.toPx(), point)
        drawCircle(line, Point.toPx(), point)
        bubble(texts, point.x, highlight)
    }
}

/** What a line of the net worth chart's bubble tells: the month, the amount, a gain or a loss. */
internal enum class BubbleLine { Label, Amount, Gain, Loss }

// The bubble over the chart, centered on [x] and kept inside it: [texts], one under the other.
private fun DrawScope.bubble(texts: List<TextLayoutResult>, x: Float, color: Color) {
    if (texts.isEmpty()) return
    val padding = BubblePadding.toPx()
    val width = texts.maxOf { it.size.width } + 2 * padding
    val height = texts.sumOf { it.size.height } + 2 * padding
    val left = (x - width / 2).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
    drawRoundRect(
        color,
        topLeft = Offset(left, 0f),
        size = Size(width, height),
        cornerRadius = CornerRadius(BubbleRadius.toPx())
    )
    var top = padding
    texts.forEach { text ->
        drawText(text, topLeft = Offset(left + padding, top))
        top += text.size.height
    }
}

/** Each of [series]' bands at each month's end, stacked from the bottom, in [colors]. */
@Composable
internal fun StackedArea(series: List<List<Double>>, colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    val highlight = MaterialTheme.colorScheme.surface
    val count = series.firstOrNull()?.size ?: 0
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(AreaHeight)
            .testTag("trends_accounts_chart")
            .monthSlots(count) { select(it) }
    ) {
        if (count < 2) return@Canvas
        val totals = (0 until count).map { month -> series.sumOf { it[month] } }
        val max = (totals.maxOrNull() ?: 0.0).takeIf { it > 0 }?.times(Headroom) ?: 1.0
        val width = size.width / count
        fun x(index: Int) = index * width + width / 2
        fun y(value: Double) = size.height - (value / max).toFloat() * size.height

        slot(selected, count, highlight)
        var below = List(count) { 0.0 }
        series.forEachIndexed { index, values ->
            val above = below.zip(values) { under, value -> under + value }
            val band = Path().apply {
                moveTo(x(0), y(above[0]))
                for (month in 1 until count) lineTo(x(month), y(above[month]))
                for (month in count - 1 downTo 0) lineTo(x(month), y(below[month]))
                close()
            }
            drawPath(band, colors[index].copy(alpha = BandAlpha))
            below = above
        }
    }
}

/** Each month's income beside its expenses, green and red as on Home's cards. */
@Composable
internal fun PairedBars(income: List<Double>, expenses: List<Double>, selected: Int, onSelect: (Int) -> Unit) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val incomeColor = incomeGreen(panel)
    val expenseColor = expenseRed(panel)
    val highlight = MaterialTheme.colorScheme.surface
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarsHeight)
            .testTag("trends_income_expense_chart")
            .monthSlots(income.size) { select(it) }
    ) {
        if (income.isEmpty()) return@Canvas
        val width = size.width / income.size
        val bar = width * PairedBarFraction
        val gap = BarGap.toPx()
        val max = maxOf(income.max(), expenses.max()).takeIf { it > 0 }?.times(Headroom) ?: 1.0
        slot(selected, income.size, highlight)
        income.indices.forEach { index ->
            val left = index * width + (width - 2 * bar - gap) / 2
            bar(incomeColor, left, bar, (income[index] / max).toFloat() * size.height)
            bar(expenseColor, left + bar + gap, bar, (expenses[index] / max).toFloat() * size.height)
        }
    }
}

// A bar standing on the chart's bottom, [height] tall.
private fun DrawScope.bar(color: Color, left: Float, width: Float, height: Float) {
    if (height <= 0) return
    drawRoundRect(
        color,
        Offset(left, size.height - height),
        Size(width, height),
        CornerRadius(minOf(width / 2, height / 2))
    )
}

/** What each month saved, up in green or, when more went out, down in red, and their average dashed. */
@Composable
internal fun SavedBars(saved: List<Double>, average: Double?, selected: Int, onSelect: (Int) -> Unit) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val up = incomeGreen(panel)
    val down = expenseRed(panel)
    val highlight = MaterialTheme.colorScheme.surface
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarsHeight)
            .testTag("trends_saved_chart")
            .monthSlots(saved.size) { select(it) }
    ) {
        if (saved.isEmpty()) return@Canvas
        val width = size.width / saved.size
        val bar = width * BarFraction
        val highest = saved.max().coerceAtLeast(0.0)
        val lowest = -saved.min().coerceAtMost(0.0)
        val span = (highest + lowest).takeIf { it > 0 } ?: 1.0
        val zero = (highest / span).toFloat() * size.height
        slot(selected, saved.size, highlight)
        saved.forEachIndexed { index, amount ->
            val height = (abs(amount) / span).toFloat() * size.height
            if (height > 0) {
                drawRoundRect(
                    if (amount >= 0) up else down,
                    topLeft = Offset(index * width + (width - bar) / 2, if (amount >= 0) zero - height else zero),
                    size = Size(bar, height),
                    cornerRadius = CornerRadius(minOf(bar / 2, height / 2)),
                )
            }
        }
        drawLine(Gray.copy(alpha = GuideAlpha), Offset(0f, zero), Offset(size.width, zero), GuideWidth.toPx())
        if (average != null) {
            val y = zero - (average / span).toFloat() * size.height
            dashed(Offset(0f, y), Offset(size.width, y))
        }
    }
}

// A grey dashed line, as averages are drawn.
private fun DrawScope.dashed(from: Offset, to: Offset) {
    drawLine(
        Gray,
        from,
        to,
        strokeWidth = AverageWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dash.toPx(), Dash.toPx())),
    )
}

/**
 * Each month's share of its income saved, a gap where nothing came in, red points below nothing
 * saved; the [average] dashed, and the chosen month's share over its point.
 */
@Composable
internal fun RateLine(rates: List<Double?>, average: Double?, selected: Int, onSelect: (Int) -> Unit) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val line = incomeGreen(panel)
    val below = expenseRed(panel)
    val highlight = MaterialTheme.colorScheme.surface
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
    )
    val chosen = rates.getOrNull(selected)?.let { measurer.measure(percent(it), label) }
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(RateHeight)
            .testTag("trends_rate_chart")
            .monthSlots(rates.size) { select(it) }
    ) {
        val known = rates.filterNotNull()
        if (known.isEmpty()) return@Canvas
        val top = (chosen?.size?.height ?: 0) + LabelGap.toPx() + PointRing.toPx()
        val bottom = size.height - PointRing.toPx()
        val max = maxOf(known.max(), 0.0) + RateMargin
        val min = minOf(known.min(), 0.0) - RateMargin
        val width = size.width / rates.size
        fun x(index: Int) = index * width + width / 2
        fun y(rate: Double) = top + ((max - rate) / (max - min)).toFloat() * (bottom - top)

        slot(selected, rates.size, highlight)
        drawLine(Gray.copy(alpha = GuideAlpha), Offset(0f, y(0.0)), Offset(size.width, y(0.0)), GuideWidth.toPx())
        average?.let { dashed(Offset(0f, y(it)), Offset(size.width, y(it))) }
        // The line runs through the months with income, broken where a month has none.
        val stroke = Stroke(RateWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        var path: Path? = null
        rates.forEachIndexed { index, rate ->
            if (rate == null) {
                path?.let { drawPath(it, line, style = stroke) }
                path = null
            } else {
                path = (path ?: Path().apply { moveTo(x(index), y(rate)) }).apply { lineTo(x(index), y(rate)) }
            }
        }
        path?.let { drawPath(it, line, style = stroke) }
        rates.forEachIndexed { index, rate ->
            if (rate == null) return@forEachIndexed
            val color = if (rate < 0) below else line
            val center = Offset(x(index), y(rate))
            if (index == selected) {
                drawCircle(panel, PointRing.toPx(), center)
                drawCircle(color, Point.toPx(), center)
            } else {
                drawCircle(color, SmallPoint.toPx(), center)
            }
        }
        val rate = rates.getOrNull(selected)
        if (chosen != null && rate != null) {
            val left = (x(selected) - chosen.size.width / 2f).coerceIn(
                0f,
                (size.width - chosen.size.width).coerceAtLeast(0f)
            )
            drawText(chosen, topLeft = Offset(left, y(rate) - chosen.size.height - LabelGap.toPx()))
        }
    }
}

/** Each month's bar of [series] stacked from the bottom, in [colors], parted by thin gaps. */
@Composable
internal fun StackedBars(
    series: List<List<Double>>,
    colors: List<Color>,
    selected: Int,
    height: Dp,
    tag: String,
    onSelect: (Int) -> Unit,
) {
    val gap = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val count = series.firstOrNull()?.size ?: 0
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .testTag(tag)
            .monthSlots(count) { select(it) }
    ) {
        if (count == 0) return@Canvas
        val width = size.width / count
        val bar = width * BarFraction
        val highest = (0 until count).maxOf { month -> series.sumOf { it[month] } }
        val max = highest.takeIf { it > 0 }?.times(Headroom) ?: 1.0
        slot(selected, count, highlight)
        for (month in 0 until count) {
            val left = month * width + (width - bar) / 2
            var bottom = size.height
            series.forEachIndexed { index, values ->
                val h = (values[month] / max).toFloat() * size.height
                if (h <= 0) return@forEachIndexed
                drawRoundRect(
                    colors[index],
                    Offset(left, bottom - h),
                    Size(bar, h),
                    CornerRadius(minOf(StackRadius.toPx(), bar / 2))
                )
                drawLine(gap, Offset(left, bottom - h), Offset(left + bar, bottom - h), StackGap.toPx())
                bottom -= h
            }
        }
    }
}

/**
 * What's spent on average on each weekday (a row each, labeled by [rows]) in each part of the day
 * (a column each, labeled by [columns]): a dot each, larger and darker for more. [strongRow] is
 * the busiest weekday's.
 */
@Composable
internal fun PunchCard(cells: List<List<Double>>, rows: List<String>, columns: List<String>, strongRow: Int?) {
    val color = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelSmall.copy(color = Gray)
    val strong = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
    )
    val columnTexts = columns.map { measurer.measure(it, label) }
    val rowTexts = rows.mapIndexed { index, row -> measurer.measure(row, if (index == strongRow) strong else label) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(PunchHeight)
            .testTag("trends_punch_card")
    ) {
        val left = (rowTexts.maxOfOrNull { it.size.width } ?: 0) + LabelGap.toPx()
        val top = (columnTexts.maxOfOrNull { it.size.height } ?: 0) + LabelGap.toPx()
        val cellWidth = (size.width - left) / columns.size
        val cellHeight = (size.height - top) / rows.size
        val max = cells.maxOf { it.max() }.takeIf { it > 0 } ?: 1.0
        val radius = minOf(cellWidth, cellHeight) / 2 - DotInset.toPx()
        columnTexts.forEachIndexed { column, text ->
            drawText(text, topLeft = Offset(left + column * cellWidth + (cellWidth - text.size.width) / 2, 0f))
        }
        rowTexts.forEachIndexed { row, text ->
            drawText(text, topLeft = Offset(0f, top + row * cellHeight + (cellHeight - text.size.height) / 2))
            cells[row].forEachIndexed { column, amount ->
                if (amount <= 0) return@forEachIndexed
                val share = (amount / max).toFloat()
                drawCircle(
                    color.copy(alpha = MinDotAlpha + (1 - MinDotAlpha) * share),
                    radius * sqrt(share),
                    Offset(left + column * cellWidth + cellWidth / 2, top + row * cellHeight + cellHeight / 2),
                )
            }
        }
    }
}

/**
 * The days of the last weeks, a column a week and Monday on top, each shaded by what was spent:
 * [shades] from none to the most; the day that cost the most outlined. The weeks fill the width,
 * their days no larger than [MaxCell].
 */
@Composable
internal fun CalendarGrid(calendar: SpendingCalendar, shades: List<Color>) {
    val outline = MaterialTheme.colorScheme.onSurface
    val weeks = (calendar.amounts.size + DaysInWeek - 1) / DaysInWeek
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = minOf((maxWidth - CellGap * (weeks - 1)) / weeks, MaxCell)
        Canvas(
            modifier = Modifier
                .width(cell * weeks + CellGap * (weeks - 1))
                .height(cell * DaysInWeek + CellGap * (DaysInWeek - 1))
                .testTag("trends_spending_calendar")
        ) {
            val side = cell.toPx()
            val gap = CellGap.toPx()
            val radius = CornerRadius(CellRadius.toPx())
            calendar.amounts.forEachIndexed { index, amount ->
                if (amount == null) return@forEachIndexed
                val topLeft = Offset((index / DaysInWeek) * (side + gap), (index % DaysInWeek) * (side + gap))
                drawRoundRect(shades[calendarLevel(amount, calendar.levels)], topLeft, Size(side, side), radius)
                if (calendar.top?.day == calendar.start.plusDays(index.toLong())) {
                    drawRoundRect(outline, topLeft, Size(side, side), radius, style = Stroke(OutlineWidth.toPx()))
                }
            }
        }
    }
}

private const val DaysInWeek = 7
private const val ChartMargin = 0.1
private const val Headroom = 1.08
private const val AreaAlpha = 0.25f
private const val ForecastAlpha = 0.45f
private const val FanAlpha = 0.2f
private const val DotLength = 0.01f
private val LikelyWidth = 3.dp
private val LikelyGap = 6.dp
private const val GuideAlpha = 0.5f
private const val BandAlpha = 0.9f
private const val BarFraction = 0.55f
private const val PairedBarFraction = 0.28f
private const val RateMargin = 0.05
private const val MinDotAlpha = 0.3f
private val NetWorthHeight = 210.dp
private val AreaHeight = 140.dp
private val BarsHeight = 130.dp
private val RateHeight = 130.dp
private val PunchHeight = 200.dp
private val MaxCell = 20.dp
private val SlotRadius = 8.dp
private val LineWidth = 3.dp
private val RateWidth = 2.5.dp
private val GuideWidth = 1.dp
private val AverageWidth = 1.5.dp
private val Dash = 4.dp
private val Point = 4.5.dp
private val PointRing = 7.dp
private val SmallPoint = 3.dp
private val BubblePadding = 8.dp
private val BubbleRadius = 12.dp
private val BubbleGap = 12.dp
private val LabelGap = 8.dp
private val BarGap = 2.dp
private val StackRadius = 4.dp
private val StackGap = 2.dp
private val DotInset = 2.dp
private val CellGap = 3.dp
private val CellRadius = 3.dp
private val OutlineWidth = 1.5.dp
