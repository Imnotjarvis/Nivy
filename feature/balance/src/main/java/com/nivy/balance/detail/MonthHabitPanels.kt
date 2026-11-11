package com.nivy.balance.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Blue
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.ui.R
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.InfoRow
import com.nivy.wallet.ui.component.InfoRows
import com.nivy.wallet.ui.component.LabeledBars
import com.nivy.wallet.ui.component.LegendText
import com.nivy.wallet.ui.component.PanelNote
import com.nivy.wallet.ui.component.ProgressBar
import com.nivy.wallet.ui.component.ShareRow
import com.nivy.wallet.ui.component.ShareRows
import com.nivy.wallet.ui.component.SplitBar
import com.nivy.wallet.ui.component.Stat
import com.nivy.wallet.ui.component.StatTiles
import com.nivy.wallet.ui.component.approxAmount
import com.nivy.wallet.ui.component.categoryColor
import com.nivy.wallet.ui.component.dayLabel
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.ui.component.weekdayName
import com.nivy.wallet.ui.component.wholeOrCents
import com.nivy.wallet.utils.amountAndCode
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.formatDecimals
import kotlinx.collections.immutable.ImmutableList
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.roundToInt

// ---- Habits

/**
 * What stands out in the month so far: categories that took more, or less, than by this day in 9
 * of the last months in 10, then purchases bigger than their category's for months.
 */
@Composable
internal fun UnusualPanel(unusual: Unusual, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(
        modifier = Modifier.testTag("month_unusual"),
        title = stringResource(R.string.month_out_of_the_ordinary),
        legend = {},
    ) {
        Column {
            unusual.purchases.forEachIndexed { index, purchase ->
                if (index > 0) Spacer(Modifier.height(16.dp))
                val category = purchase.category?.name?.value
                val since = purchase.since
                InfoRow(
                    dot = categoryColor(purchase.category),
                    title = purchase.title ?: category ?: stringResource(R.string.unspecified),
                    noteLines = 2,
                    note = "${dayLabel(purchase.day)}, " + when {
                        since == null && category != null ->
                            stringResource(R.string.month_biggest_in, category, purchase.months)
                        since == null -> stringResource(R.string.month_biggest_any_in, purchase.months)
                        category != null ->
                            stringResource(R.string.month_biggest_since, category, sinceName(since, purchase.day))
                        else -> stringResource(R.string.month_biggest_any_since, sinceName(since, purchase.day))
                    },
                    value = amountAndCode(purchase.amount, currency),
                )
            }
            unusual.categories.forEachIndexed { index, item ->
                if (index > 0 || unusual.purchases.isNotEmpty()) Spacer(Modifier.height(16.dp))
                val name = item.category?.name?.value ?: stringResource(R.string.unspecified)
                val usual = approxAmount(item.usual)
                InfoRow(
                    dot = categoryColor(item.category),
                    title = name,
                    noteLines = 2,
                    note = when {
                        item.beaten == item.months && item.higher ->
                            stringResource(R.string.month_usually_highest, usual, currency, item.months)
                        item.beaten == item.months ->
                            stringResource(R.string.month_usually_lowest, usual, currency, item.months)
                        item.higher ->
                            stringResource(R.string.month_usually_higher, usual, currency, item.beaten, item.months)
                        else -> stringResource(R.string.month_usually_lower, usual, currency, item.beaten, item.months)
                    },
                    value = amountAndCode(item.amount, currency),
                    valueColor = if (item.higher) expenseRed(panel) else incomeGreen(panel),
                )
            }
        }
    }
}

// The month a purchase is the biggest since, with its year when it's another than the purchase's.
@Composable
@ReadOnlyComposable
private fun sinceName(since: YearMonth, day: LocalDate): String {
    val name = Month.fromMonthValue(since.monthValue).displayName()
    return if (since.year == day.year) name else "$name ${since.year}"
}

@Composable
internal fun HabitTiles(habits: Habits, currency: String, lastDay: LocalDate?, outlook: MonthOutlook? = null) {
    StatTiles(
        modifier = Modifier.testTag("month_habit_tiles"),
        stats = buildList {
            add(
                Stat(
                    stringResource(R.string.month_spent_a_day),
                    amountAndCode(habits.perDay, currency),
                    pluralStringResource(R.plurals.month_on_average_days, habits.days, habits.days),
                )
            )
            val likely = outlook?.spending?.lastOrNull()
            if (lastDay != null && likely != null) {
                add(
                    Stat(
                        stringResource(R.string.month_likely_spent),
                        "${approxAmount(likely.likely)} $currency",
                        stringResource(R.string.month_by_day, dayLabel(lastDay)),
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
                        amountAndCode(it.amount, currency),
                        "$what, ${dayLabel(it.day)}"
                    )
                )
            }
            add(
                Stat(
                    stringResource(R.string.month_purchases),
                    "${habits.purchases}",
                    stringResource(R.string.month_a_day, (habits.purchases.toDouble() / habits.days).formatDecimals(1)),
                )
            )
            val median = habits.median
            val average = habits.average
            if (median != null && average != null) {
                add(
                    Stat(
                        stringResource(R.string.month_typical_purchase),
                        amountAndCode(median, currency),
                        stringResource(R.string.month_average_amount, average.format(currency), currency),
                    )
                )
            }
            habits.busiest?.let {
                add(
                    Stat(
                        stringResource(R.string.month_busiest_day),
                        weekdayName(it, TextStyle.FULL),
                        stringResource(
                            R.string.month_of_spending,
                            percent(habits.byWeekday.max() / habits.chosenSpent),
                        ),
                    )
                )
            }
            habits.weekendRatio?.let {
                add(
                    Stat(
                        stringResource(R.string.month_weekends),
                        stringResource(R.string.month_times, it.formatDecimals(1)),
                        stringResource(R.string.month_weekday_spending),
                    )
                )
            }
            add(
                Stat(
                    stringResource(R.string.month_no_spend_days),
                    "${habits.noSpendDays}",
                    pluralStringResource(R.plurals.month_of_days_so_far, habits.trackedDays, habits.trackedDays),
                )
            )
            add(
                Stat(
                    stringResource(R.string.month_small_purchases),
                    stringResource(
                        R.string.month_under,
                        habits.small.count,
                        wholeOrCents(habits.small.under),
                        currency,
                    ),
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
internal fun WhenYouSpendPanel(habits: Habits) {
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
internal fun PurchaseSizesPanel(habits: Habits, currency: String) {
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
                    null -> "${wholeOrCents(band.from)}+"
                    else -> if (band.from == 0.0) {
                        "<${wholeOrCents(to)}"
                    } else {
                        "${wholeOrCents(band.from)}-${wholeOrCents(to)}"
                    }
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
                wholeOrCents(habits.small.under),
                currency,
                percent(habits.small.share),
            )
        )
    }
}

@Composable
internal fun FixedFlexiblePanel(habits: Habits, currency: String) {
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
            value = amountAndCode(habits.fixed.amount, currency),
            trailing = percent(habits.fixed.amount / total),
        )
        Spacer(Modifier.height(12.dp))
        InfoRow(
            dot = flexibleColor,
            title = stringResource(R.string.month_flexible),
            note = pluralStringResource(R.plurals.month_other_purchases, habits.flexible.count, habits.flexible.count),
            value = amountAndCode(habits.flexible.amount, currency),
            trailing = percent(habits.flexible.amount / total),
        )
    }
}

@Composable
internal fun CategoriesPanel(habits: Habits, currency: String, previous: Pace?) {
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
                value = amountAndCode(change.amount, currency),
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

/** Each budget: its limit, a bar of what was spent of it, and what's left of it or over it. */
@Composable
internal fun BudgetsPanel(budgets: ImmutableList<BudgetMonth>, currency: String) {
    val panel = MaterialTheme.colorScheme.surfaceVariant
    ChartPanel(title = stringResource(R.string.budgets), legend = {}) {
        budgets.forEachIndexed { index, budget ->
            if (index > 0) Spacer(Modifier.height(16.dp))
            val over = budget.spent > budget.limit
            InfoRow(title = budget.name, value = amountAndCode(budget.limit, currency))
            Spacer(Modifier.height(8.dp))
            ProgressBar(budget.spent / budget.limit, if (over) expenseRed(panel) else Nivy)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                LegendText(stringResource(R.string.month_spent_amount, budget.spent.format(currency), currency))
                Spacer(Modifier.weight(1f))
                if (over) {
                    Text(
                        text = stringResource(
                            R.string.month_budget_over,
                            (budget.spent - budget.limit).format(currency),
                            currency,
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(color = expenseRed(panel)),
                    )
                } else {
                    LegendText(
                        stringResource(
                            R.string.month_left_amount,
                            (budget.limit - budget.spent).format(currency),
                            currency,
                        )
                    )
                }
            }
        }
    }
}
