package com.nivy.balance.detail

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.nivy.data.model.Account
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.minimalTone
import com.nivy.design.system.colors.toComposeColor
import com.nivy.ui.R
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.ui.component.ChartPanel
import com.nivy.wallet.ui.component.GroupTitle
import com.nivy.wallet.ui.component.InfoRow
import com.nivy.wallet.ui.component.InfoRows
import com.nivy.wallet.ui.component.SplitBar
import com.nivy.wallet.ui.component.percent
import com.nivy.wallet.utils.amountAndCode
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.formatDecimals
import kotlin.math.floor

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
            FreeToSpendPanel(it, currency, month, detail.outlook, detail.days.lastOrNull())
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
            PacePanel(it, currency, month, detail.outlook, detail.days)
        }
    }

    detail.habits?.let { habits ->
        GroupTitle(stringResource(R.string.month_your_habits))
        detail.unusual?.let {
            UnusualPanel(it, currency)
            Spacer(Modifier.height(16.dp))
        }
        HabitTiles(habits, currency, detail.days.lastOrNull(), detail.outlook)
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
internal fun SubLabel(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(color = Gray),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun BigNumber(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(color = color, fontWeight = FontWeight.ExtraBold),
    )
}

@Composable
internal fun Subtext(text: String, small: Boolean = false) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        style = (if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium)
            .copy(color = Gray),
    )
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
                    value = amountAndCode(holding.amount, currency),
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
                    value = "-${amountAndCode(-holding.amount, currency)}",
                    valueColor = expenseRed(panel),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = stringResource(R.string.net_worth), style = totalStyle())
            Spacer(Modifier.weight(1f))
            Text(text = amountAndCode(holdings.netWorth, currency), style = totalStyle())
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
                stringResource(R.string.month_years, (runway.months / MonthsInYear).formatDecimals(1))
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
internal fun partColor(rank: Int, color: Color): Color = if (ColorMode.minimal) minimalTone(rank) else color

// An account's or a category's own color, its grey in minimal colors.
private fun accountColor(account: Account): Color = account.color.value.toComposeColor()

private const val YearsFrom = 24
private const val MonthsInYear = 12.0
