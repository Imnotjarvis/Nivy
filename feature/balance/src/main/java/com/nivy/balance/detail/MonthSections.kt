package com.nivy.balance.detail

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.design.system.colors.Blue
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.minimalTone
import com.nivy.design.system.colors.toComposeColor
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.ui.component.AxisLabels
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.GroupTitle
import com.nivy.wallet.ui.component.InfoRow
import com.nivy.wallet.ui.component.InfoRows
import com.nivy.wallet.ui.component.LabeledBars
import com.nivy.wallet.ui.component.LegendDot
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.ProgressBar
import com.nivy.wallet.ui.component.ShareRow
import com.nivy.wallet.ui.component.ShareRows
import com.nivy.wallet.ui.component.SplitBar
import com.nivy.wallet.ui.component.Stat
import com.nivy.wallet.ui.component.StatTiles
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.ui.component.transaction.label
import com.nivy.wallet.utils.format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The month in detail, under the net worth screen's balances: its days, then the habits its
 * purchases show, then the money as it is now. A section shows when it has something to tell.
 */
@Composable
internal fun ColumnScope.MonthSections(detail: MonthDetail, currency: String, month: Month?) {
    if (detail.days.isNotEmpty()) {
        GroupTitle(
            if (detail.today != null) stringResource(R.string.month_this_month) else month?.displayName().orEmpty()
        )
        detail.freeToSpend?.let {
            FreeToSpendPanel(it, currency, month)
            Spacer(Modifier.height(16.dp))
        }
        DayByDayPanel(detail, currency)
        if (detail.comingUp.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            ComingUpPanel(detail, currency)
        }
        if (detail.actualDays > 0) {
            Spacer(Modifier.height(16.dp))
            SpentEachDayPanel(detail, currency)
        }
        detail.pace?.let {
            Spacer(Modifier.height(16.dp))
            PacePanel(it, currency, month)
        }
    }

    detail.habits?.let { habits ->
        GroupTitle(stringResource(R.string.month_your_habits))
        HabitTiles(habits, currency, detail.days.lastOrNull())
        Spacer(Modifier.height(16.dp))
        WhenYouSpendPanel(habits)
        Spacer(Modifier.height(16.dp))
        PurchaseSizesPanel(habits, currency)
        Spacer(Modifier.height(16.dp))
        FixedFlexiblePanel(habits, currency)
        Spacer(Modifier.height(16.dp))
        CategoriesPanel(habits, currency, detail.pace?.takeIf { pace -> (pace.previous.lastOrNull() ?: 0.0) > 0 })
        if (habits.places.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            PlacesPanel(habits, currency)
        }
    }

    GroupTitle(stringResource(R.string.month_your_money))
    OwnAndOwePanel(detail.holdings, currency)
    detail.runway?.let {
        Spacer(Modifier.height(16.dp))
        RunwayPanel(it, currency)
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(color = Gray),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BigNumber(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(color = color, fontWeight = FontWeight.ExtraBold),
    )
}

@Composable
private fun Subtext(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(color = Gray),
    )
}

private fun money(amount: Double, currency: String) = "${amount.format(currency)} $currency"

@Composable
@ReadOnlyComposable
private fun dateLabel(day: LocalDate): String = with(LocalTimeFormatter.current) {
    day.atStartOfDay().format(TimeFormatter.Style.DateOnly(includeWeekDay = false))
}

private fun weekdayName(day: DayOfWeek, style: TextStyle): String = day.getDisplayName(style, Locale.getDefault())

// ---- This month

@Composable
private fun FreeToSpendPanel(free: FreeToSpend, currency: String, month: Month?) {
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
            ProgressBar(free.spent / (free.spent + free.left).coerceAtLeast(Double.MIN_VALUE), Nivy)
        } else {
            BigNumber("-${money(-free.left, currency)}", expenseRed(panel))
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
    }
}

@Composable
private fun DayByDayPanel(detail: MonthDetail, currency: String) {
    val planned = detail.actualDays < detail.days.size
    ChartPanel(
        title = stringResource(R.string.month_day_by_day),
        legend = {
            LegendDot(color = Nivy, label = stringResource(R.string.month_balance))
            if (planned) {
                Spacer(Modifier.width(12.dp))
                LegendDot(color = Orange, label = stringResource(R.string.month_planned))
            }
        },
    ) {
        MonthBalanceChart(detail, currency)
        val last = detail.days.lastIndex.coerceAtLeast(1)
        val today = detail.today?.let { detail.days.indexOf(it) }?.takeIf { it >= 0 }?.toFloat()?.div(last)
        AxisLabels(
            buildList {
                add(0f to dateLabel(detail.days.first()))
                // Today, unless it's so near an end that the labels would meet.
                if (today != null && today > EdgeRoom && today < 1 - EdgeRoom) {
                    add(today to stringResource(R.string.today))
                }
                add(1f to dateLabel(detail.days.last()))
            }
        )
        detail.lowest?.let { lowest ->
            Spacer(Modifier.height(12.dp))
            PanelNote(
                stringResource(
                    R.string.month_lowest_point,
                    lowest.amount.format(currency),
                    currency,
                    dateLabel(lowest.day)
                ),
                color = Gray,
            )
        }
    }
}

@Composable
private fun ComingUpPanel(detail: MonthDetail, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val count = detail.comingUp.size + detail.moreComingUp
    ChartPanel(
        title = stringResource(R.string.month_coming_up),
        legend = { LegendText(pluralStringResource(R.plurals.month_payments, count, count)) },
    ) {
        InfoRows(detail.comingUp) { upcoming ->
            InfoRow(
                lead = dateLabel(upcoming.day),
                title = upcoming.title ?: upcoming.category?.name?.value ?: stringResource(upcoming.type.label()),
                note = upcoming.account?.name?.value,
                value = when (upcoming.type) {
                    TransactionType.INCOME -> "+${money(upcoming.amount, currency)}"
                    TransactionType.EXPENSE -> "-${money(abs(upcoming.amount), currency)}"
                    TransactionType.TRANSFER -> money(upcoming.amount, currency)
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
private fun SpentEachDayPanel(detail: MonthDetail, currency: String) {
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
private fun PacePanel(pace: Pace, currency: String, month: Month?) {
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
        PaceChart(pace)
        val count = maxOf(pace.previous.size, pace.current.size)
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
        val by = pace.comparedTo?.let { dateLabel(it) }
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
    }
}

// ---- Habits

@Composable
private fun HabitTiles(habits: Habits, currency: String, lastDay: LocalDate?) {
    StatTiles(
        modifier = Modifier.testTag("month_habit_tiles"),
        stats = buildList {
            add(
                Stat(
                    stringResource(R.string.month_spent_a_day),
                    money(habits.perDay, currency),
                    pluralStringResource(R.plurals.month_on_average_days, habits.days, habits.days),
                )
            )
            if (lastDay != null && habits.days < habits.daysInMonth) {
                add(
                    Stat(
                        stringResource(R.string.month_at_this_pace),
                        money(habits.perDay * habits.daysInMonth, currency),
                        stringResource(R.string.month_spent_by, dateLabel(lastDay)),
                    )
                )
            }
            habits.savedShare?.let {
                add(Stat(stringResource(R.string.month_saved), percent(it), stringResource(R.string.month_of_income)))
            }
            habits.biggest?.let {
                val what = it.title ?: it.category?.name?.value ?: stringResource(R.string.unspecified)
                add(
                    Stat(
                        stringResource(R.string.month_biggest_expense),
                        money(it.amount, currency),
                        "$what, ${dateLabel(it.day)}"
                    )
                )
            }
            add(
                Stat(
                    stringResource(R.string.month_purchases),
                    "${habits.purchases}",
                    stringResource(R.string.month_a_day, (habits.purchases.toDouble() / habits.days).format(1)),
                )
            )
            add(
                Stat(
                    stringResource(R.string.month_typical_purchase),
                    money(habits.median, currency),
                    stringResource(R.string.month_average_amount, habits.average.format(currency), currency),
                )
            )
            habits.busiest?.let {
                add(
                    Stat(
                        stringResource(R.string.month_busiest_day),
                        weekdayName(it, TextStyle.FULL),
                        stringResource(R.string.month_of_spending, percent(habits.byWeekday.max() / habits.spent)),
                    )
                )
            }
            habits.weekendRatio?.let {
                add(
                    Stat(
                        stringResource(R.string.month_weekends),
                        stringResource(R.string.month_times, it.format(1)),
                        stringResource(R.string.month_weekday_spending),
                    )
                )
            }
            add(
                Stat(
                    stringResource(R.string.month_no_spend_days),
                    "${habits.noSpendDays}",
                    pluralStringResource(R.plurals.month_of_days_so_far, habits.days, habits.days),
                )
            )
            add(
                Stat(
                    stringResource(R.string.month_small_purchases),
                    stringResource(R.string.month_under, habits.small.count, compact(habits.small.under), currency),
                    stringResource(R.string.month_of_spending, percent(habits.small.share)),
                )
            )
            habits.topPlace?.let {
                add(
                    Stat(
                        stringResource(R.string.month_top_place),
                        it.name,
                        pluralStringResource(R.plurals.month_visits, it.visits, it.visits),
                    )
                )
            }
            habits.topAccount?.let { use ->
                use.account?.let { account ->
                    add(
                        Stat(
                            stringResource(R.string.month_most_used_account),
                            account.name.value,
                            pluralStringResource(
                                R.plurals.month_of_payments,
                                habits.purchases,
                                use.payments,
                                habits.purchases
                            ),
                        )
                    )
                }
            }
        },
    )
}

@Composable
private fun WhenYouSpendPanel(habits: Habits) {
    val red = expenseRed(MaterialTheme.colorScheme.surfaceVariant)
    ChartPanel(title = stringResource(R.string.month_when_you_spend), legend = {}) {
        SubLabel(stringResource(R.string.month_by_weekday))
        LabeledBars(
            values = habits.byWeekday,
            labels = DayOfWeek.entries.map { weekdayName(it, TextStyle.SHORT) },
            color = red,
            highlight = habits.busiest?.ordinal,
        )
        habits.timeOfDay?.let { shares ->
            Spacer(Modifier.height(20.dp))
            SubLabel(stringResource(R.string.month_by_time_of_day))
            val parts = listOf(
                R.string.month_morning to (6 to 12),
                R.string.month_afternoon to (12 to 18),
                R.string.month_evening to (18 to 24),
                R.string.month_night to (0 to 6),
            )
            ShareRows(
                rows = parts.mapIndexed { index, (name, hours) ->
                    ShareRow(
                        stringResource(name),
                        stringResource(R.string.month_hours, hours.first, hours.second),
                        shares[index]
                    )
                },
                color = red,
                highlight = shares.indexOf(shares.max()),
            )
        }
    }
}

@Composable
private fun PurchaseSizesPanel(habits: Habits, currency: String) {
    ChartPanel(
        title = stringResource(R.string.month_purchase_sizes),
        legend = {
            LegendText(
                pluralStringResource(R.plurals.month_purchases_count, habits.purchases, habits.purchases)
            )
        },
    ) {
        LabeledBars(
            values = habits.sizes.map { it.count.toDouble() },
            labels = habits.sizes.map { band ->
                when (val to = band.to) {
                    null -> "${compact(band.from)}+"
                    else -> if (band.from == 0.0) "<${compact(to)}" else "${compact(band.from)}-${compact(to)}"
                }
            },
            color = Nivy,
            notes = habits.sizes.map { percent(it.share) },
            height = 88.dp,
        )
        Spacer(Modifier.height(12.dp))
        PanelNote(
            pluralStringResource(
                R.plurals.month_sizes_note,
                habits.purchases,
                habits.small.count,
                habits.purchases,
                compact(habits.small.under),
                currency,
                percent(habits.small.share),
            )
        )
    }
}

@Composable
private fun FixedFlexiblePanel(habits: Habits, currency: String) {
    val total = (habits.fixed.amount + habits.flexible.amount).takeIf { it > 0 } ?: 1.0
    ChartPanel(title = stringResource(R.string.month_fixed_and_flexible), legend = {}) {
        val fixedColor = partColor(0, Nivy)
        val flexibleColor = partColor(1, Blue)
        SplitBar(listOf(habits.fixed.amount to fixedColor, habits.flexible.amount to flexibleColor))
        Spacer(Modifier.height(16.dp))
        InfoRow(
            dot = fixedColor,
            title = stringResource(R.string.month_fixed),
            note = pluralStringResource(R.plurals.month_planned_payments, habits.fixed.count, habits.fixed.count),
            value = money(habits.fixed.amount, currency),
            trailing = percent(habits.fixed.amount / total),
        )
        Spacer(Modifier.height(12.dp))
        InfoRow(
            dot = flexibleColor,
            title = stringResource(R.string.month_flexible),
            note = pluralStringResource(R.plurals.month_other_purchases, habits.flexible.count, habits.flexible.count),
            value = money(habits.flexible.amount, currency),
            trailing = percent(habits.flexible.amount / total),
        )
    }
}

@Composable
private fun CategoriesPanel(habits: Habits, currency: String, previous: Pace?) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        title = stringResource(R.string.categories),
        legend = {
            previous?.previousMonth?.let { LegendText(stringResource(R.string.month_versus, it.displayName())) }
        },
    ) {
        InfoRows(habits.categories) { change ->
            InfoRow(
                dot = categoryColor(change.category),
                title = change.category?.name?.value ?: stringResource(R.string.unspecified),
                value = money(change.amount, currency),
                trailing = when {
                    previous == null -> null
                    change.change == null -> stringResource(R.string.month_new)
                    (change.change * 100).roundToInt() == 0 -> stringResource(R.string.month_same)
                    change.change > 0 -> "+${percent(change.change)}"
                    else -> "-${percent(-change.change)}"
                },
                // Spending more is red, as an expense is; less, green.
                trailingColor = when {
                    change.change == null || (change.change * 100).roundToInt() == 0 -> Gray
                    change.change > 0 -> expenseRed(panel)
                    else -> incomeGreen(panel)
                },
            )
        }
    }
}

@Composable
private fun PlacesPanel(habits: Habits, currency: String) {
    ChartPanel(title = stringResource(R.string.month_places), legend = {}) {
        InfoRows(habits.places.withIndex().toList()) { (index, place) ->
            InfoRow(
                lead = "${index + 1}",
                leadWidth = RankWidth,
                title = place.name,
                note = stringResource(
                    R.string.month_place_note,
                    place.visits,
                    (place.total / place.visits).format(currency),
                    currency
                ),
                value = money(place.total, currency),
            )
        }
    }
}

// ---- Money

@Composable
private fun OwnAndOwePanel(holdings: Holdings, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        modifier = Modifier.testTag("month_own_and_owe"),
        title = stringResource(R.string.month_own_and_owe),
        legend = {},
    ) {
        if (holdings.assets.isNotEmpty()) {
            val owned = holdings.assets.sumOf { it.amount }
            val colors = holdings.assets.map { accountColor(it.account) }
            SplitBar(holdings.assets.zip(colors) { holding, color -> holding.amount to color })
            Spacer(Modifier.height(16.dp))
            InfoRows(holdings.assets.zip(colors)) { (holding, color) ->
                InfoRow(
                    dot = color,
                    title = holding.account.name.value,
                    value = money(holding.amount, currency),
                    trailing = percent(holding.amount / owned),
                )
            }
        }
        if (holdings.debts.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SubLabel(stringResource(R.string.month_you_owe))
            InfoRows(holdings.debts) { holding ->
                InfoRow(
                    dot = expenseRed(panel),
                    title = holding.account.name.value,
                    value = "-${money(-holding.amount, currency)}",
                    valueColor = expenseRed(panel),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = stringResource(R.string.net_worth), style = totalStyle())
            Spacer(Modifier.weight(1f))
            Text(text = money(holdings.netWorth, currency), style = totalStyle())
        }
    }
}

@Composable
@ReadOnlyComposable
private fun totalStyle() = MaterialTheme.typography.bodyMedium.copy(
    color = MaterialTheme.colorScheme.onSurface,
    fontWeight = FontWeight.ExtraBold,
)

@Composable
private fun RunwayPanel(runway: Runway, currency: String) {
    ChartPanel(title = stringResource(R.string.month_money_lasts), legend = {}) {
        val months = floor(runway.months).toInt()
        BigNumber(
            if (months < YearsFrom) {
                pluralStringResource(R.plurals.month_months, months.coerceAtLeast(1), months.coerceAtLeast(1))
            } else {
                stringResource(R.string.month_years, (runway.months / MonthsInYear).format(1))
            }
        )
        Spacer(Modifier.height(4.dp))
        Subtext(
            pluralStringResource(
                R.plurals.month_lasts_note,
                runway.over,
                runway.monthly.format(currency),
                currency,
                runway.over
            )
        )
    }
}

// A part of a split bar that isn't an account, and its row's dot: its color, or in minimal colors a
// grey by its place.
private fun partColor(rank: Int, color: Color): Color = if (ColorMode.minimal) minimalTone(rank) else color

// An account's or a category's own color, its grey in minimal colors.
private fun accountColor(account: Account): Color = account.color.value.toComposeColor()

private fun categoryColor(category: Category?): Color = category?.color?.value?.toComposeColor() ?: Gray

/** An amount without its decimals when it's whole, as a size band's edge. */
private fun compact(amount: Double): String = if (amount == floor(amount)) "%,.0f".format(amount) else amount.format(2)

private const val EdgeRoom = 0.12f
private val RankWidth = 24.dp
private const val DaysInWeek = 7
private const val ShortMonth = 3
private const val YearsFrom = 24
private const val MonthsInYear = 12.0
