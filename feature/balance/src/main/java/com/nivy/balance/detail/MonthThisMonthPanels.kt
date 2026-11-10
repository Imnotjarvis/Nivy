package com.nivy.balance.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.ui.R
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.domain.pure.forecast.Band
import com.nivy.wallet.ui.component.AxisLabels
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.InfoRow
import com.nivy.wallet.ui.component.InfoRows
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.ProgressBar
import com.nivy.wallet.ui.component.approxAmount
import com.nivy.wallet.ui.component.dayLabel
import com.nivy.wallet.ui.component.transaction.label
import com.nivy.wallet.utils.amountAndCode
import com.nivy.wallet.utils.format
import java.time.LocalDate
import kotlin.math.abs

// ---- This month

@Composable
internal fun FreeToSpendPanel(
    free: FreeToSpend,
    currency: String,
    month: Month?,
    outlook: MonthOutlook? = null,
    lastDay: LocalDate? = null,
) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        modifier = Modifier.testTag("month_free_to_spend"),
        title = stringResource(R.string.month_free_to_spend),
        legend = {},
    ) {
        if (free.left >= 0) {
            BigNumber(stringResource(R.string.month_per_day, free.perDay.format(currency), currency))
            Spacer(Modifier.height(4.dp))
            Subtext(
                pluralStringResource(
                    R.plurals.month_days_left_even,
                    free.daysLeft,
                    free.daysLeft,
                    month?.displayName().orEmpty(),
                )
            )
            Spacer(Modifier.height(16.dp))
            val whole = (free.spent + free.left).coerceAtLeast(Double.MIN_VALUE)
            val likely = outlook?.left?.let { free.left - it.likely }
            if (likely != null) {
                SpendBar(free.spent / whole, (free.spent + likely) / whole, Nivy)
            } else {
                ProgressBar(free.spent / whole, Nivy)
            }
        } else {
            BigNumber("-${amountAndCode(-free.left, currency)}", expenseRed(panel))
            Spacer(Modifier.height(4.dp))
            Subtext(stringResource(R.string.month_over))
            Spacer(Modifier.height(16.dp))
            ProgressBar(1.0, expenseRed(panel))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = stringResource(R.string.month_spent_amount, free.spent.format(currency), currency),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(Modifier.weight(1f))
            if (free.left >= 0) {
                LegendText(stringResource(R.string.month_left_amount, free.left.format(currency), currency))
            }
        }
        val left = outlook?.left
        if (left != null && lastDay != null) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(
                    color = Nivy.copy(alpha = LikelyAlpha),
                    label = stringResource(R.string.month_usually_a_day, approxAmount(outlook.perDay), currency),
                )
            }
            Spacer(Modifier.height(8.dp))
            PanelNote(
                text = if (left.likely >= 0) {
                    stringResource(R.string.month_likely_left, approxAmount(left.likely), currency, dayLabel(lastDay))
                } else {
                    stringResource(R.string.month_likely_short, approxAmount(-left.likely), currency, dayLabel(lastDay))
                },
                color = if (left.likely >= 0) incomeGreen(panel) else expenseRed(panel),
            )
            Spacer(Modifier.height(2.dp))
            Subtext(
                when {
                    left.low >= 0 -> rangeText(left, currency)
                    left.high < 0 -> stringResource(
                        R.string.month_likely_range_short,
                        approxAmount(-left.high),
                        approxAmount(-left.low),
                        currency,
                    )
                    else -> stringResource(
                        R.string.month_likely_range_either,
                        approxAmount(-left.low),
                        approxAmount(left.high),
                        currency,
                    )
                },
                small = true,
            )
        }
    }
}

@Composable
internal fun DayByDayPanel(detail: MonthDetail, currency: String) {
    val planned = detail.actualDays < detail.days.size
    ChartPanel(
        title = stringResource(R.string.month_day_by_day),
        legend = {
            LegendDot(color = Nivy, label = stringResource(R.string.month_balance))
            if (planned) {
                Spacer(Modifier.width(12.dp))
                LegendDot(color = Orange, label = stringResource(R.string.month_planned))
            }
            if (detail.outlook != null) {
                Spacer(Modifier.width(12.dp))
                LegendDot(color = Nivy.copy(alpha = LikelyAlpha), label = stringResource(R.string.month_likely))
            }
        },
    ) {
        MonthBalanceChart(detail, currency)
        val last = detail.days.lastIndex.coerceAtLeast(1)
        val today = detail.today?.let { detail.days.indexOf(it) }?.takeIf { it >= 0 }?.toFloat()?.div(last)
        AxisLabels(
            buildList {
                add(0f to dayLabel(detail.days.first()))
                // Today, unless it's so near an end that the labels would meet.
                if (today != null && today > EdgeRoom && today < 1 - EdgeRoom) {
                    add(today to stringResource(R.string.today))
                }
                add(1f to dayLabel(detail.days.last()))
            }
        )
        val likelyLowest = detail.outlook?.lowest
        if (likelyLowest != null) {
            Spacer(Modifier.height(12.dp))
            PanelNote(
                stringResource(
                    if (likelyLowest.day == detail.days.last()) {
                        R.string.month_likely_by
                    } else {
                        R.string.month_likely_lowest
                    },
                    approxAmount(likelyLowest.likely),
                    currency,
                    dayLabel(likelyLowest.day),
                ),
            )
            Spacer(Modifier.height(2.dp))
            Subtext(stringResource(R.string.month_stays_above, approxAmount(likelyLowest.low), currency), small = true)
        }
        detail.lowest?.takeIf { likelyLowest == null }?.let { lowest ->
            Spacer(Modifier.height(12.dp))
            PanelNote(
                stringResource(
                    R.string.month_lowest_point,
                    lowest.amount.format(currency),
                    currency,
                    dayLabel(lowest.day)
                ),
                color = Gray,
            )
        }
    }
}

@Composable
internal fun ComingUpPanel(detail: MonthDetail, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val count = detail.comingUp.size + detail.moreComingUp
    ChartPanel(
        title = stringResource(R.string.month_coming_up),
        legend = { LegendText(pluralStringResource(R.plurals.month_payments, count, count)) },
    ) {
        InfoRows(detail.comingUp) { upcoming ->
            InfoRow(
                lead = dayLabel(upcoming.day),
                title = upcoming.title ?: upcoming.category?.name?.value ?: stringResource(upcoming.type.label()),
                note = upcoming.account?.name?.value,
                value = when (upcoming.type) {
                    TransactionType.INCOME -> "+${amountAndCode(upcoming.amount, currency)}"
                    TransactionType.EXPENSE -> "-${amountAndCode(abs(upcoming.amount), currency)}"
                    TransactionType.TRANSFER -> amountAndCode(upcoming.amount, currency)
                },
                valueColor = when (upcoming.type) {
                    TransactionType.INCOME -> incomeGreen(panel)
                    TransactionType.EXPENSE -> expenseRed(panel)
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (detail.moreComingUp > 0) {
            Spacer(Modifier.height(12.dp))
            PanelNote(stringResource(R.string.month_more_coming_up, detail.moreComingUp), color = Gray)
        }
    }
}

@Composable
internal fun SpentEachDayPanel(detail: MonthDetail, currency: String) {
    ChartPanel(
        title = stringResource(R.string.month_spent_each_day),
        legend = {
            LegendDot(
                color = expenseRed(MaterialTheme.colorScheme.surfaceVariant),
                label = stringResource(R.string.month_spent)
            )
            if (detail.plannedDays.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                LegendDot(color = Orange, label = stringResource(R.string.month_planned))
            }
        },
    ) {
        DailyBars(detail, detail.habits?.perDay, currency)
        // A label a week, and the last day's.
        val last = detail.days.lastIndex
        val marks = (0..last step DaysInWeek).filter { last - it >= DaysInWeek / 2 } + last
        AxisLabels(marks.map { (it + 0.5f) / detail.days.size to "${detail.days[it].dayOfMonth}" })
    }
}

@Composable
internal fun PacePanel(
    pace: Pace,
    currency: String,
    month: Month?,
    outlook: MonthOutlook? = null,
    days: List<LocalDate> = emptyList(),
) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val previousName = pace.previousMonth?.displayName().orEmpty()
    ChartPanel(
        title = stringResource(R.string.month_compared_with, previousName),
        legend = {
            LegendDot(color = expenseRed(panel), label = month?.displayName()?.take(ShortMonth).orEmpty())
            Spacer(Modifier.width(12.dp))
            LegendDot(color = Gray, label = previousName.take(ShortMonth))
        },
    ) {
        val ahead = outlook?.spending?.takeIf { days.isNotEmpty() }
        PaceChart(pace, ahead, days.size)
        val count = maxOf(pace.previous.size, pace.current.size, if (ahead != null) days.size else 0)
        val day = pace.current.lastIndex
        AxisLabels(
            buildList {
                add(0f to "1")
                val at = day / (count - 1f).coerceAtLeast(1f)
                if (at > EdgeRoom && at < 1 - EdgeRoom) add(at to "${day + 1}")
                add(1f to "$count")
            }
        )
        val before = pace.previous.getOrNull(day)
        val by = pace.comparedTo?.let { dayLabel(it) }
        if (before != null && by != null) {
            Spacer(Modifier.height(12.dp))
            val difference = before - pace.current.last()
            PanelNote(
                text = if (difference >= 0) {
                    stringResource(R.string.month_less_than_by, difference.format(currency), currency, by)
                } else {
                    stringResource(R.string.month_more_than_by, (-difference).format(currency), currency, by)
                },
                color = if (difference >= 0) incomeGreen(panel) else expenseRed(panel),
            )
        }
        val end = ahead?.lastOrNull()
        if (end != null) {
            Spacer(Modifier.height(12.dp))
            val previousTotal = pace.previous.lastOrNull()
            val likely =
                stringResource(R.string.month_likely_by, approxAmount(end.likely), currency, dayLabel(days.last()))
            PanelNote(
                text = if (previousTotal != null && previousTotal > 0) {
                    val difference = end.likely - previousTotal
                    if (difference >= 0) {
                        stringResource(
                            R.string.month_likely_more,
                            likely,
                            approxAmount(difference),
                            currency,
                            previousName,
                        )
                    } else {
                        stringResource(
                            R.string.month_likely_less,
                            likely,
                            approxAmount(-difference),
                            currency,
                            previousName,
                        )
                    }
                } else {
                    likely
                },
            )
            Spacer(Modifier.height(2.dp))
            Subtext(rangeText(end, currency), small = true)
        }
    }
}

// 8 runs in 10 between the band's edges.
@Composable
@ReadOnlyComposable
private fun rangeText(band: Band, currency: String): String =
    stringResource(R.string.month_likely_range, approxAmount(band.low), approxAmount(band.high), currency)

/** A track filled to [spent], from 0 to 1, and lighter on to [likely]. */
@Composable
private fun SpendBar(spent: Double, likely: Double, color: Color) {
    val track = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(12.dp)
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, size = size, cornerRadius = radius)
        drawRoundRect(
            color.copy(alpha = LikelyAlpha),
            size = Size(likely.coerceIn(0.0, 1.0).toFloat() * size.width, size.height),
            cornerRadius = radius,
        )
        drawRoundRect(
            color,
            size = Size(spent.coerceIn(0.0, 1.0).toFloat() * size.width, size.height),
            cornerRadius = radius,
        )
    }
}

private const val EdgeRoom = 0.12f
private const val LikelyAlpha = 0.35f
private const val DaysInWeek = 7
private const val ShortMonth = 3
