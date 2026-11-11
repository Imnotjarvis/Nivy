package com.nivy.wallet.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

private const val ApproxFrom = 100.0
private const val ApproxStep = 10.0
