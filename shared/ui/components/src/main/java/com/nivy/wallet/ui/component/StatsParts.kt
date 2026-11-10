package com.nivy.wallet.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Category
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.toComposeColor
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.utils.format
import java.text.DecimalFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// What the two stats screens, the month in detail and Trends, write and draw alike.

/** [day]'s date, without its weekday. */
@Composable
@ReadOnlyComposable
fun dayLabel(day: LocalDate): String = with(LocalTimeFormatter.current) {
    day.atStartOfDay().format(TimeFormatter.Style.DateOnly(includeWeekDay = false))
}

/** A weekday's name in the app's language, read from the composition so a change of language redraws it. */
@Composable
@ReadOnlyComposable
fun weekdayName(day: DayOfWeek, style: TextStyle): String =
    day.getDisplayName(style, LocalConfiguration.current.locales[0])

/** A category's own color, and grey for none. */
fun categoryColor(category: Category?): Color = category?.color?.value?.toComposeColor() ?: Gray

/** An amount without its decimals when it's whole, as a size band's edge. */
fun wholeOrCents(amount: Double): String =
    if (amount == floor(amount)) "%,.0f".format(amount) else "%,.2f".format(amount)

/** A forecast's amount, as round as it deserves: to 10 from 100 on, whole below. */
fun approxAmount(amount: Double): String {
    val step = if (abs(amount) >= ApproxFrom) ApproxStep else 1.0
    return DecimalFormat("#,##0").format((amount / step).roundToInt() * step)
}

/**
 * A row of what stands out: a [dot], a [title] over a [note] of up to two lines, a [value], and a
 * [trailing] change, when there is one.
 */
@Composable
fun FindingRow(
    dot: Color,
    title: String,
    note: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: String? = null,
    trailingColor: Color = Gray,
) {
    Row(modifier = modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
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
        Text(text = value, maxLines = 1, style = MaterialTheme.typography.bodyMedium.copy(color = valueColor))
        if (trailing != null) {
            Text(
                modifier = Modifier.width(56.dp),
                text = trailing,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(color = trailingColor, fontWeight = FontWeight.Bold),
            )
        }
    }
}

private const val ApproxFrom = 100.0
private const val ApproxStep = 10.0
