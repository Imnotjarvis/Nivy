package com.nivy.wallet.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// What the charts of the stats screens, and of an account's value, draw alike.

/** The width of a chart's main line. */
val ChartLineWidth = 3.dp

/** A marked point on a chart's line. */
val ChartPoint = 4.5.dp

/** The ring around a marked point, which a chart also leaves room for at its edges. */
val ChartPointRing = 7.dp

/** The alpha of the band an outlook's likely values stay within. */
const val LikelyBandAlpha = 0.2f

/**
 * A line through [points] in [color], over an area in that color that fades out from [top] down to
 * [bottom], where it ends.
 */
fun DrawScope.lineOverArea(points: List<Offset>, color: Color, top: Float = 0f, bottom: Float = size.height) {
    if (points.isEmpty()) return
    val line = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (index in 1..points.lastIndex) lineTo(points[index].x, points[index].y)
    }
    val area = Path().apply {
        addPath(line)
        lineTo(points.last().x, bottom)
        lineTo(points.first().x, bottom)
        close()
    }
    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = AreaAlpha), Color.Transparent), top, bottom))
    drawPath(line, color, style = Stroke(ChartLineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** The stroke of what's likely ahead, dotted: apart from what's planned, dashed, in minimal colors too. */
fun DrawScope.likelyStroke(): Stroke = Stroke(
    LikelyWidth.toPx(),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(DotLength, LikelyGap.toPx())),
)

/** A point on a line, in a [ring] of the panel's color that parts it from the line. */
fun DrawScope.pointInRing(
    center: Offset,
    color: Color,
    ring: Color,
    radius: Dp = ChartPoint,
    ringRadius: Dp = ChartPointRing,
) {
    drawCircle(ring, ringRadius.toPx(), center)
    drawCircle(color, radius.toPx(), center)
}

private const val AreaAlpha = 0.25f
private const val DotLength = 0.01f
private val LikelyWidth = 3.dp
private val LikelyGap = 6.dp
