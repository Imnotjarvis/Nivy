package com.nivy.transactions

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.ui.R
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.LegendDot
import kotlinx.collections.immutable.ImmutableList
import com.nivy.wallet.ui.component.lineOverArea

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
            LegendDot(color = Nivy, label = stringResource(R.string.value))
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
        lineOverArea(values, Nivy)
        values.forEachIndexed { index, point ->
            val radius = if (index == values.lastIndex) PointRadius * 2 else PointRadius
            drawCircle(Nivy, radius = radius.toPx(), center = point)
        }
    }
}

private const val MonthLabelLength = 3
private val PointRadius = 3.dp
private val DashWidth = 2.dp
private val Dash = 5.dp
