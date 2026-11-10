package com.nivy.balance.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.expenseRed
import com.nivy.ui.R
import com.nivy.wallet.domain.pure.forecast.Band
import com.nivy.wallet.utils.format
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The month's balance, a day a step: as it was up to today, then, dashed, as planned payments
 * will take it. Today and the month's last day are marked, with their balances. With an outlook,
 * the balance ahead is also drawn as likely, less the everyday spending to come, in a band that 8
 * runs in 10 stay within.
 */
@Composable
internal fun MonthBalanceChart(detail: MonthDetail, currency: String) {
    val line = Nivy
    val planned = Orange
    val guide = Gray
    val ring = MaterialTheme.colorScheme.surfaceVariant
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
    )
    val balances = detail.balances
    val actual = detail.actualDays
    // Only while the month runs: it has days so far and days ahead.
    val outlook = detail.outlook
    val todayText = balances.getOrNull(actual - 1)?.let { measurer.measure(it.format(currency), label) }
    val endText = if (actual < balances.size) measurer.measure(balances.last().format(currency), label) else null
    val likelyText = outlook?.balances?.lastOrNull()?.let { measurer.measure(approx(it.likely), label) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (outlook != null) 184.dp else 160.dp)
            .testTag("month_balance_chart")
    ) {
        if (balances.size < 2) return@Canvas
        val values = balances + outlook?.balances?.flatMap { listOf(it.low, it.high) }.orEmpty()
        val margin = (values.max() - values.min()).coerceAtLeast(1.0) * ChartMargin
        val min = values.min() - margin
        val max = values.max() + margin
        val top = LabelSpace.toPx()
        val bottom = size.height - PointRing.toPx() - (likelyText?.let { it.size.height + LabelGap.toPx() } ?: 0f)
        fun x(index: Int) = index / (balances.size - 1f) * size.width
        fun y(value: Double) = top + ((max - value) / (max - min)).toFloat() * (bottom - top)

        if (actual > 0) {
            val path = Path().apply {
                moveTo(x(0), y(balances[0]))
                for (index in 1 until actual) lineTo(x(index), y(balances[index]))
            }
            val area = Path().apply {
                addPath(path)
                lineTo(x(actual - 1), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = AreaAlpha), Color.Transparent), top, bottom))
            drawPath(path, line, style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (outlook != null) likelyAhead(outlook.balances, actual - 1, ::x, ::y, line, ring, likelyText)
        if (actual < balances.size) {
            plannedAhead(balances, (actual - 1).coerceAtLeast(0), ::x, ::y, planned, ring, endText)
        }
        if (actual > 0) {
            val today = Offset(x(actual - 1), y(balances[actual - 1]))
            if (actual < balances.size) {
                drawLine(
                    guide.copy(alpha = GuideAlpha),
                    Offset(today.x, top - PointRing.toPx()),
                    Offset(today.x, bottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dot.toPx(), Dot.toPx())),
                )
            }
            point(today, line, ring)
            todayText?.let { text -> labelAbove(text, today) }
        }
    }
}

/**
 * What was spent each day of the month: today's slot stands out, the days ahead are stubs with a
 * dot where an expense is planned, and a dashed line marks the average so far.
 */
@Composable
internal fun DailyBars(detail: MonthDetail, average: Double?, currency: String) {
    val spent = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
    val highlight = MaterialTheme.colorScheme.surface
    val idle = Gray.copy(alpha = IdleAlpha)
    val planned = Orange
    val measurer = rememberTextMeasurer()
    val averageText = average?.let {
        measurer.measure(
            stringResource(R.string.month_average_short, it.format(currency)),
            MaterialTheme.typography.labelSmall.copy(color = Gray),
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag("month_daily_bars")
    ) {
        val days = detail.days
        if (days.isEmpty()) return@Canvas
        val slot = size.width / days.size
        val bar = slot * DayBarFraction
        val bottom = size.height - DotSpace.toPx()
        val max = (detail.spentByDay.maxOrNull() ?: 0.0).takeIf { it > 0 }?.times(BarHeadroom) ?: 1.0
        fun height(amount: Double) = (amount / max).toFloat() * (bottom - BarTop.toPx())
        val todayIndex = detail.today?.let(days::indexOf)?.takeIf { it >= 0 }
        if (todayIndex != null) {
            drawRoundRect(
                highlight,
                topLeft = Offset(todayIndex * slot, 0f),
                size = Size(slot, size.height),
                cornerRadius = CornerRadius(SlotRadius.toPx()),
            )
        }
        days.forEachIndexed { index, day ->
            val left = index * slot + (slot - bar) / 2
            val amount = detail.spentByDay[index]
            val happened = index < detail.actualDays
            val h = if (happened) height(amount).coerceAtLeast(Stub.toPx()) else Stub.toPx()
            drawRoundRect(
                if (happened && amount > 0) spent else idle,
                topLeft = Offset(left, bottom - h),
                size = Size(bar, h),
                cornerRadius = CornerRadius(bar / 2),
            )
            if (day in detail.plannedDays) {
                drawCircle(planned, PlannedDot.toPx(), Offset(left + bar / 2, size.height - PlannedDot.toPx()))
            }
        }
        if (average != null && average > 0 && detail.actualDays > 0) {
            val y = bottom - height(average)
            val end = detail.actualDays * slot
            drawLine(
                Gray,
                Offset(0f, y),
                Offset(end, y),
                strokeWidth = AverageWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dash.toPx() * 0.8f, Dash.toPx() * 0.8f)),
            )
            averageText?.let { text ->
                val x = (end + LabelGap.toPx()).coerceAtMost(size.width - text.size.width)
                drawText(text, topLeft = Offset(x, y - text.size.height / 2f))
            }
        }
    }
}

/**
 * What was spent by each day, added up: this month so far over the whole month before. With the
 * [ahead] bands, from today to the month's [days], the month's likely spending to its end too.
 */
@Composable
internal fun PaceChart(pace: Pace, ahead: List<Band>? = null, days: Int = 0) {
    val now = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
    val before = Gray
    val ring = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .testTag("month_pace_chart")
    ) {
        val count = maxOf(pace.previous.size, pace.current.size, if (ahead != null) days else 0)
        if (count < 2) return@Canvas
        val highest = maxOf(pace.previous.lastOrNull() ?: 0.0, pace.current.last(), ahead?.lastOrNull()?.high ?: 0.0)
        val max = highest.takeIf { it > 0 }?.times(BarHeadroom) ?: 1.0
        val top = PointRing.toPx()
        val bottom = size.height - PointRing.toPx()
        fun x(index: Int) = index / (count - 1f) * size.width
        fun y(value: Double) = bottom - (value / max).toFloat() * (bottom - top)
        fun path(values: List<Double>) = Path().apply {
            moveTo(x(0), y(values.first()))
            for (index in 1..values.lastIndex) lineTo(x(index), y(values[index]))
        }
        if (pace.previous.size > 1) {
            drawPath(
                path(pace.previous),
                before,
                style = Stroke(PlannedWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        val day = pace.current.lastIndex
        if (ahead != null && ahead.size > 1) likelyAhead(ahead, day, ::x, ::y, now, ring, label = null, small = true)
        if (pace.current.size > 1) {
            drawPath(
                path(pace.current),
                now,
                style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        pace.previous.getOrNull(day)?.let { point(Offset(x(day), y(it)), before, ring, small = true) }
        point(Offset(x(day), y(pace.current.last())), now, ring)
    }
}

/** A forecast's amount, as round as it deserves: to 10 from 100 on, whole below. */
internal fun approx(amount: Double): String {
    val step = if (abs(amount) >= ApproxFrom) ApproxStep else 1.0
    return DecimalFormat("#,##0").format((amount / step).roundToInt() * step)
}

/**
 * The [balances] from the step [from] on, as planned: each holds until a planned payment's day, then
 * steps to it. The last is marked, with [label] over it.
 */
private fun DrawScope.plannedAhead(
    balances: List<Double>,
    from: Int,
    x: (Int) -> Float,
    y: (Double) -> Float,
    color: Color,
    ring: Color,
    label: TextLayoutResult?,
) {
    val path = Path().apply {
        moveTo(x(from), y(balances[from]))
        for (index in from + 1 until balances.size) {
            lineTo(x(index), y(balances[index - 1]))
            lineTo(x(index), y(balances[index]))
        }
    }
    drawPath(
        path,
        color,
        style = Stroke(
            PlannedWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dash.toPx(), Dash.toPx())),
        ),
    )
    val end = Offset(x(balances.lastIndex), y(balances.last()))
    point(end, color, ring)
    label?.let { drawText(it, topLeft = Offset(size.width - it.size.width, end.y - it.size.height - LabelGap.toPx())) }
}

/**
 * From the step [from] on, [bands] a step: the band 8 runs in 10 stay within, the likely line
 * through it, dotted, and its end, marked, with [label] under it.
 */
private fun DrawScope.likelyAhead(
    bands: List<Band>,
    from: Int,
    x: (Int) -> Float,
    y: (Double) -> Float,
    color: Color,
    ring: Color,
    label: TextLayoutResult?,
    small: Boolean = false,
) {
    val band = Path().apply {
        moveTo(x(from), y(bands[0].high))
        for (step in 1..bands.lastIndex) lineTo(x(from + step), y(bands[step].high))
        for (step in bands.lastIndex downTo 0) lineTo(x(from + step), y(bands[step].low))
        close()
    }
    drawPath(band, color.copy(alpha = BandAlpha))
    val likely = Path().apply {
        moveTo(x(from), y(bands[0].likely))
        for (step in 1..bands.lastIndex) lineTo(x(from + step), y(bands[step].likely))
    }
    drawPath(likely, color, style = dotted())
    val end = Offset(x(from + bands.lastIndex), y(bands.last().likely))
    point(end, color, ring, small)
    label?.let { drawText(it, topLeft = Offset(size.width - it.size.width, end.y + LabelGap.toPx())) }
}

// What's likely, dotted: apart from what's planned, dashed, in minimal colors too.
private fun DrawScope.dotted() = Stroke(
    LikelyWidth.toPx(),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(DotLength, LikelyGap.toPx())),
)

// A point on a line, in a ring of the panel's color that parts it from the line.
private fun DrawScope.point(center: Offset, color: Color, ring: Color, small: Boolean = false) {
    drawCircle(ring, (if (small) SmallRing else PointRing).toPx(), center)
    drawCircle(color, (if (small) SmallPoint else Point).toPx(), center)
}

// A label centered over a point, kept inside the chart.
private fun DrawScope.labelAbove(text: TextLayoutResult, point: Offset) {
    val x = (point.x - text.size.width / 2f).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f))
    drawText(text, topLeft = Offset(x, point.y - text.size.height - LabelGap.toPx()))
}

private const val ChartMargin = 0.15
private const val BandAlpha = 0.2f
private const val DotLength = 0.01f
private val LikelyWidth = 3.dp
private val LikelyGap = 6.dp
private const val ApproxFrom = 100.0
private const val ApproxStep = 10.0
private const val AreaAlpha = 0.25f
private const val GuideAlpha = 0.5f
private const val IdleAlpha = 0.25f
private const val DayBarFraction = 0.6f
private const val BarHeadroom = 1.1
private val LabelSpace = 28.dp
private val LabelGap = 8.dp
private val LineWidth = 3.dp
private val PlannedWidth = 2.5.dp
private val AverageWidth = 1.5.dp
private val Dash = 4.dp
private val Dot = 2.dp
private val PointRing = 7.dp
private val Point = 4.5.dp
private val SmallRing = 6.dp
private val SmallPoint = 3.5.dp
private val SlotRadius = 6.dp
private val DotSpace = 10.dp
private val BarTop = 8.dp
private val Stub = 2.dp
private val PlannedDot = 3.dp
