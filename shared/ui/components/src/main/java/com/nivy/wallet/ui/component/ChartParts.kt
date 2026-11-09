package com.nivy.wallet.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import kotlin.math.roundToInt

// The small pieces the stats screens draw their panels with: axis labels, bars, shares, rows and
// tiles. Inside a [ChartPanel], rows sit 8 dp in, under its title.

/** Labels under a chart, each centered on its place across it, from 0 to 1, and kept inside it. */
@Composable
fun AxisLabels(
    labels: List<Pair<Float, String>>,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        content = {
            labels.forEachIndexed { index, (_, text) ->
                Text(
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface else Gray,
                        fontWeight = if (index == selectedIndex) FontWeight.ExtraBold else FontWeight.Normal,
                    ),
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
            placeables.forEachIndexed { index, placeable ->
                val x = (labels[index].first * width - placeable.width / 2f).roundToInt()
                placeable.place(x.coerceIn(0, (width - placeable.width).coerceAtLeast(0)), 0)
            }
        }
    }
}

/** [labels] centered on equal slots across a chart, as its bars are. */
fun evenly(labels: List<String>): List<Pair<Float, String>> =
    labels.mapIndexed { index, label -> (index + 0.5f) / labels.size to label }

/**
 * A bar for each of [values] over its label; [notes], when given, under the labels. The
 * [highlight]ed slot stands out on the panel, as a chosen month does on Trends.
 */
@Composable
fun LabeledBars(
    values: List<Double>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    notes: List<String>? = null,
    height: Dp = 96.dp,
) {
    val slotColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val slot = size.width / values.size
        val bar = slot * BarFraction
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        if (highlight != null) {
            drawRoundRect(
                slotColor,
                topLeft = Offset(highlight * slot, 0f),
                size = Size(slot, size.height),
                cornerRadius = CornerRadius(SlotRadius.toPx()),
            )
        }
        values.forEachIndexed { index, value ->
            val barHeight = ((value / max).toFloat() * (size.height - BarTopSpace.toPx()))
                .coerceAtLeast(MinBarHeight.toPx())
            drawRoundRect(
                if (value == 0.0) color.copy(alpha = EmptyAlpha) else color,
                topLeft = Offset(index * slot + (slot - bar) / 2, size.height - barHeight),
                size = Size(bar, barHeight),
                cornerRadius = CornerRadius(minOf(bar / 2, BarRadius.toPx())),
            )
        }
    }
    AxisLabels(evenly(labels), selectedIndex = highlight)
    if (notes != null) AxisLabels(evenly(notes))
}

/** A row of [ShareRows]: its [label] over a [note], and its [share] of the whole, from 0 to 1. */
@Immutable
data class ShareRow(val label: String, val note: String, val share: Double)

/** Each row's label, a bar of its share, the largest full, and the share in percent. */
@Composable
fun ShareRows(rows: List<ShareRow>, color: Color, highlight: Int? = null) {
    val track = MaterialTheme.colorScheme.surface
    val max = rows.maxOfOrNull { it.share }?.takeIf { it > 0 } ?: 1.0
    rows.forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(108.dp)) {
                Text(
                    text = row.label,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (index == highlight) FontWeight.ExtraBold else FontWeight.Bold,
                    ),
                )
                Text(text = row.note, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(color = Gray))
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
            ) {
                drawRoundRect(track, size = size, cornerRadius = CornerRadius(size.height / 2))
                drawRoundRect(
                    color,
                    size = Size((row.share / max).toFloat() * size.width, size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            }
            Text(
                modifier = Modifier.width(52.dp),
                text = percent(row.share),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

/** One bar split into [parts], each its amount's share of the bar, in its color. */
@Composable
fun SplitBar(parts: List<Pair<Double, Color>>, modifier: Modifier = Modifier) {
    val gap = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(16.dp)
    ) {
        val total = parts.sumOf { it.first }.takeIf { it > 0 } ?: return@Canvas
        var left = 0f
        parts.forEach { (amount, color) ->
            val width = (amount / total).toFloat() * size.width
            drawRoundRect(
                color,
                topLeft = Offset(left, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(size.height / 2),
            )
            left += width
            // The panel's color between parts, so that each reads on its own.
            drawLine(gap, Offset(left, 0f), Offset(left, size.height), strokeWidth = PartGap.toPx())
        }
    }
}

/** A track filled to [fraction], from 0 to 1. */
@Composable
fun ProgressBar(fraction: Double, color: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(12.dp)
    ) {
        drawRoundRect(track, size = size, cornerRadius = CornerRadius(size.height / 2))
        drawRoundRect(
            color,
            size = Size(fraction.coerceIn(0.0, 1.0).toFloat() * size.width, size.height),
            cornerRadius = CornerRadius(size.height / 2),
        )
    }
}

/**
 * A row of a list in a panel: a [dot] of color or a short [lead], [leadWidth] wide, a [title] over
 * a [note], a [value], and a [trailing] share or change. A title or a note too long ends in an
 * ellipsis.
 */
@Composable
fun InfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    lead: String? = null,
    leadWidth: Dp = 56.dp,
    dot: Color? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: String? = null,
    trailingColor: Color = Gray,
) {
    Row(modifier = modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(dot, MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.width(10.dp))
        }
        if (lead != null) {
            Text(
                modifier = Modifier.width(leadWidth),
                text = lead,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(color = Gray),
            )
        }
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
            if (note != null) {
                Text(
                    text = note,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(color = Gray),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text = value, maxLines = 1, style = MaterialTheme.typography.bodyMedium.copy(color = valueColor))
        if (trailing != null) {
            Text(
                modifier = Modifier.width(60.dp),
                text = trailing,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(color = trailingColor, fontWeight = FontWeight.Bold),
            )
        }
    }
}

/** [items], each as [row] draws it, one under the other. */
@Composable
fun <T> InfoRows(items: List<T>, row: @Composable (T) -> Unit) {
    items.forEachIndexed { index, item ->
        if (index > 0) Spacer(Modifier.height(12.dp))
        row(item)
    }
}

/** A short note under a chart, in a panel. */
@Composable
fun PanelNote(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        modifier = modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = FontWeight.Bold),
    )
}

/** A stat's small grey [label], its [value], a [note] under it and, when given, its [trend]. */
@Immutable
data class Stat(
    val label: String,
    val value: String,
    val note: String,
    val trend: List<Double>? = null,
)

/** [stats] as tiles, two a row, 16 dp from the screen's edges. */
@Composable
fun StatTiles(stats: List<Stat>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        stats.chunked(2).forEachIndexed { row, pair ->
            if (row > 0) Spacer(Modifier.height(12.dp))
            Row {
                pair.forEachIndexed { index, stat ->
                    if (index > 0) Spacer(Modifier.width(12.dp))
                    StatTile(stat)
                }
                // An odd last tile keeps its width.
                if (pair.size == 1) {
                    Spacer(Modifier.width(12.dp))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatTile(stat: Stat) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = stat.label, maxLines = 1, style = MaterialTheme.typography.labelMedium.copy(color = Gray))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stat.value,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(Modifier.height(2.dp))
        Text(text = stat.note, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(color = Gray))
        val trend = stat.trend
        if (trend != null && trend.size > 1) {
            Spacer(Modifier.height(10.dp))
            Sparkline(trend, Nivy)
        }
    }
}

/** A small line of [values], its last one marked, scaled from their lowest to their highest. */
@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val min = values.min()
        val span = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val step = size.width / (values.size - 1)
        fun y(value: Double) = size.height - ((value - min) / span).toFloat() * size.height
        val line = Path().apply {
            values.forEachIndexed { index, value ->
                if (index == 0) moveTo(0f, y(value)) else lineTo(index * step, y(value))
            }
        }
        drawPath(line, color, style = Stroke(SparkWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, SparkPoint.toPx(), Offset(size.width, y(values.last())))
    }
}

/** A share, from 0 to 1, in whole percent. */
fun percent(share: Double): String = "${(share * 100).roundToInt()}%"

private const val BarFraction = 0.5f
private const val EmptyAlpha = 0.25f
private val SlotRadius = 8.dp
private val BarRadius = 6.dp
private val BarTopSpace = 10.dp
private val MinBarHeight = 2.dp
private val PartGap = 3.dp
private val SparkWidth = 2.dp
private val SparkPoint = 3.dp
