package com.nivy.reports.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.expenseRed
import com.nivy.ui.R
import com.nivy.wallet.domain.pure.spending.DayParts
import com.nivy.wallet.domain.pure.spending.EveningPart
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.Stat
import com.nivy.wallet.ui.component.amountWithCode
import com.nivy.wallet.ui.component.dayLabel
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.ui.component.weekdayName
import com.nivy.wallet.ui.component.wholeOrCents
import com.nivy.wallet.utils.format
import java.time.DayOfWeek
import java.time.format.TextStyle
import kotlin.math.roundToInt

// ---- Habits

@Composable
@ReadOnlyComposable
internal fun habitStats(months: List<MonthTrend>, habits: RangeHabits, currency: String, phrase: String): List<Stat> {
    val full = months.filter { it.full }
    return buildList {
        if (full.isNotEmpty()) {
            val purchases = full.map { it.habits.purchases.toDouble() }
            add(
                Stat(
                    label = stringResource(R.string.month_purchases),
                    value = stringResource(R.string.trends_a_month, purchases.average().roundToInt().toString()),
                    note = changeNote(trendChange(purchases), phrase),
                    trend = purchases,
                )
            )
        }
        habits.typical?.let { typical ->
            val typicals = full.mapNotNull { it.habits.typical }
            add(
                Stat(
                    label = stringResource(R.string.month_typical_purchase),
                    value = amountWithCode(typical, currency),
                    note = changeNote(trendChange(typicals), phrase),
                    trend = typicals,
                )
            )
        }
        if (full.isNotEmpty()) add(noSpendStat(full))
        addAll(shareStats(full, habits, currency))
    }
}

@Composable
@ReadOnlyComposable
private fun noSpendStat(full: List<MonthTrend>): Stat {
    val days = full.map { it.habits.noSpendDays.toDouble() }
    val best = full.maxBy { it.habits.noSpendDays }
    return Stat(
        label = stringResource(R.string.month_no_spend_days),
        value = stringResource(R.string.trends_a_month, days.average().roundToInt().toString()),
        note = stringResource(R.string.trends_best_month, best.habits.noSpendDays, monthName(best.period)),
        trend = days,
    )
}

// The shares of the spending on weekends, in small purchases and in the evening.
@Composable
@ReadOnlyComposable
private fun shareStats(full: List<MonthTrend>, habits: RangeHabits, currency: String): List<Stat> = buildList {
    habits.weekendShare?.let { share ->
        add(
            Stat(
                label = stringResource(R.string.month_weekends),
                value = stringResource(R.string.month_of_spending, percent(share)),
                note = stringResource(R.string.trends_on_days, percent(habits.weekendDays)),
                trend = full.mapNotNull { month -> month.habits.weekend.shareOf(month.habits.spent) },
            )
        )
    }
    habits.smallShare?.let { share ->
        add(
            Stat(
                label = stringResource(R.string.month_small_purchases),
                value = stringResource(R.string.month_of_spending, percent(share)),
                note = stringResource(R.string.trends_under_each, wholeOrCents(habits.smallUnder), currency),
                trend = full.mapNotNull { month -> month.habits.small.shareOf(month.habits.spent) },
            )
        )
    }
    habits.eveningShare?.let { share ->
        val evening = DayParts[EveningPart]
        add(
            Stat(
                label = stringResource(R.string.trends_evenings),
                value = stringResource(R.string.month_of_spending, percent(share)),
                note = stringResource(R.string.month_hours, evening.first, evening.last + 1),
                trend = full.mapNotNull { month -> month.habits.evening.shareOf(month.habits.timed) },
            )
        )
    }
}

private fun Double.shareOf(whole: Double): Double? = if (whole > 0) this / whole else null

@Composable
@ReadOnlyComposable
private fun changeNote(change: Double?, phrase: String): String =
    if (change == null) {
        stringResource(R.string.trends_on_average)
    } else {
        stringResource(R.string.trends_change_over, signedPercent(change), phrase)
    }

@Composable
internal fun WhenPanel(cells: List<List<Double>>, currency: String) {
    val busiest = cells.indices.flatMap { row -> cells[row].indices.map { column -> row to column } }
        .maxBy { (row, column) -> cells[row][column] }
    val weekdays = DayOfWeek.entries
    val parts =
        listOf(
            R.string.trends_mornings,
            R.string.trends_afternoons,
            R.string.trends_evenings_part,
            R.string.trends_nights
        )
    ChartPanel(
        title = stringResource(R.string.month_when_you_spend),
        legend = { LegendText(stringResource(R.string.trends_on_average)) },
    ) {
        PunchCard(
            cells = cells,
            rows = weekdays.map { weekdayName(it, TextStyle.SHORT) },
            columns = listOf(
                R.string.month_morning,
                R.string.month_afternoon,
                R.string.month_evening,
                R.string.month_night
            )
                .map { stringResource(it) },
            strongRow = busiest.first,
        )
        Spacer(Modifier.height(12.dp))
        val (row, column) = busiest
        PanelNote(
            text = stringResource(
                R.string.trends_most_on,
                weekdayName(weekdays[row], TextStyle.FULL),
                stringResource(parts[column]),
                cells[row][column].format(currency),
                currency,
            ),
        )
    }
}

@Composable
internal fun CalendarPanel(calendar: SpendingCalendar, currency: String) {
    val red = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
    val shades = listOf(MaterialTheme.colorScheme.surface) +
        (1..CalendarShades).map { red.copy(alpha = shadeAlpha(it)) }
    val weeks = (calendar.amounts.size + DaysInWeek - 1) / DaysInWeek
    ChartPanel(
        title = stringResource(R.string.trends_spending_calendar),
        legend = { LegendText(pluralStringResource(R.plurals.trends_last_weeks, weeks, weeks)) },
    ) {
        Box(Modifier.padding(horizontal = 8.dp)) { CalendarGrid(calendar, shades) }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.trends_less), style = MaterialTheme.typography.labelSmall.copy(color = Gray))
            Spacer(Modifier.width(6.dp))
            shades.forEach { shade ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .background(shade, MaterialTheme.shapes.extraSmall)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.trends_more), style = MaterialTheme.typography.labelSmall.copy(color = Gray))
        }
        calendar.top?.let { top ->
            Spacer(Modifier.height(12.dp))
            PanelNote(
                text = stringResource(
                    R.string.trends_most_spent_day,
                    weekdayDate(top.day),
                    top.amount.format(currency),
                    currency
                ),
            )
        }
    }
}

// ---- Records

@Composable
@ReadOnlyComposable
internal fun recordStats(records: Records, currency: String): List<Stat> = buildList {
    records.mostSaved?.let {
        add(
            Stat(
                stringResource(R.string.trends_most_saved),
                "+${amountWithCode(it.amount, currency)}",
                monthYear(it.period),
            )
        )
    }
    records.mostSpent?.let {
        add(Stat(stringResource(R.string.trends_most_spent), amountWithCode(it.amount, currency), monthYear(it.period)))
    }
    records.biggest?.let {
        val what = it.title ?: it.category?.name?.value ?: stringResource(R.string.unspecified)
        add(
            Stat(
                stringResource(R.string.trends_biggest_purchase),
                amountWithCode(it.amount, currency),
                stringResource(R.string.trends_what_when, what, dayLabel(it.day)),
            )
        )
    }
    records.noSpendStreak?.let {
        add(
            Stat(
                stringResource(R.string.trends_no_spend_streak),
                pluralStringResource(R.plurals.trends_days, it.days, it.days),
                stringResource(R.string.trends_from_to, dayLabel(it.from), dayLabel(it.to)),
            )
        )
    }
    records.cheapestWeek?.let {
        add(
            Stat(
                stringResource(R.string.trends_cheapest_week),
                amountWithCode(it.amount, currency),
                stringResource(
                    R.string.trends_from_to,
                    dayLabel(it.from),
                    dayLabel(it.from.plusDays(DaysInWeek - 1L))
                ),
            )
        )
    }
    records.busiestDay?.let {
        add(
            Stat(
                stringResource(R.string.month_busiest_day),
                pluralStringResource(R.plurals.month_purchases_count, it.purchases, it.purchases),
                weekdayDate(it.day),
            )
        )
    }
}

private const val CalendarShades = 4
