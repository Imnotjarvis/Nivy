package com.ivy.transactions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Ivy
import com.ivy.ui.R
import com.ivy.wallet.ui.component.ChartPanel
import com.ivy.wallet.ui.component.LegendDot
import kotlinx.collections.immutable.ImmutableList

/**
 * An investment account's value at each month's end, as the net worth line on Trends, over the money
 * invested, a dashed step: the gap between them is the gain.
 */
@Composable
internal fun ValueChart(
    points: ImmutableList<ValuePoint>,
    modifier: Modifier = Modifier,
) {
    ChartPanel(
        modifier = modifier.testTag("account_value_chart"),
        title = stringResource(R.string.value),
        legend = {
            LegendDot(color = Ivy, label = stringResource(R.string.value))
            Spacer(Modifier.width(12.dp))
            LegendDot(color = Gray, label = stringResource(R.string.invested))
        },
    ) {
        ValueLines(points)
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { index, point ->
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    text = point.month?.displayName()?.take(MonthLabelLength).orEmpty(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        // The current month, as the last one on Trends.
                        color = if (index == points.lastIndex) MaterialTheme.colorScheme.onSurface else Gray,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ValueLines(points: ImmutableList<ValuePoint>) {
    val all = points.flatMap { listOf(it.value, it.invested) }
    val min = all.min()
    val span = (all.max() - min).takeIf { it > 0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val slot = size.width / points.size
        val padding = PointRadius.toPx() * 3
        val chartHeight = size.height - 2 * padding
        fun x(index: Int) = index * slot + slot / 2
        fun y(amount: Double) = padding + ((min + span - amount) / span).toFloat() * chartHeight

        val invested = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(x(index), y(point.invested))
                } else {
                    lineTo(x(index), y(points[index - 1].invested))
                    lineTo(x(index), y(point.invested))
                }
            }
        }
        drawPath(
            invested,
            color = Gray,
            style = Stroke(
                width = DashWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dash.toPx(), Dash.toPx())),
                join = StrokeJoin.Round,
            ),
        )

        val values = points.mapIndexed { index, point -> Offset(x(index), y(point.value)) }
        val line = Path().apply {
            values.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(values.last().x, size.height)
            lineTo(values.first().x, size.height)
            close()
        }
        drawPath(area, brush = Brush.verticalGradient(listOf(Ivy.copy(alpha = AreaAlpha), Color.Transparent)))
        drawPath(
            line,
            color = Ivy,
            style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        values.forEachIndexed { index, point ->
            val radius = if (index == values.lastIndex) PointRadius * 2 else PointRadius
            drawCircle(Ivy, radius = radius.toPx(), center = point)
        }
    }
}

private const val AreaAlpha = 0.25f
private const val MonthLabelLength = 3
private val PointRadius = 3.dp
private val LineWidth = 3.dp
private val DashWidth = 2.dp
private val Dash = 5.dp
