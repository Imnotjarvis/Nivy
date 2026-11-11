package com.nivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.shown
import com.nivy.design.system.motion.appearTween
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.typography.numberTitleLarge
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.home.usecase.Glance
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.component.transaction.TransactionsDividerLine
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB1
import com.nivy.wallet.ui.theme.wallet.backToThisMonth
import com.nivy.wallet.utils.HiddenAmount
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.horizontalSwipeListener
import com.nivy.wallet.utils.rememberSwipeListenerState
import com.nivy.wallet.utils.springBounce
import kotlin.math.absoluteValue

/**
 * How Home's header collapses as the list scrolls, and expands back at its top. The menu button
 * beside it, which Home draws on its own, moves with it on the same spring.
 */
internal fun <T> headerSpring() = springBounce<T>(stiffness = Spring.StiffnessLow)

@ExperimentalAnimationApi
@Composable
internal fun HomeHeader(
    expanded: Boolean,
    glance: Glance?,
    hideIncome: Boolean,
    period: TimePeriod,
    onShowMonthModal: () -> Unit,
    onSelectNextMonth: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    onSelectPeriod: (TimePeriod) -> Unit,
    firstGlanceFact: GlanceFact? = null,
) {
    Column {
        val percentExpanded by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = headerSpring(),
            label = "Home Header Expand Collapse"
        )

        Spacer(Modifier.height(20.dp))

        HeaderStickyRow(
            percentExpanded = percentExpanded,
            glance = glance,
            hideIncome = hideIncome,
            firstGlanceFact = firstGlanceFact,
            period = period,

            onShowMonthModal = onShowMonthModal,
            onSelectNextMonth = onSelectNextMonth,
            onSelectPreviousMonth = onSelectPreviousMonth,
            onSelectPeriod = onSelectPeriod,
        )

        Spacer(Modifier.height(16.dp))

        if (percentExpanded < 0.5f) {
            TransactionsDividerLine(
                modifier = Modifier.alpha(1f - percentExpanded),
                paddingHorizontal = 0.dp
            )
        }
    }
}

@Composable
private fun HeaderStickyRow(
    percentExpanded: Float,
    glance: Glance?,
    hideIncome: Boolean,
    firstGlanceFact: GlanceFact?,
    period: TimePeriod,
    onShowMonthModal: () -> Unit,
    onSelectNextMonth: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    onSelectPeriod: (TimePeriod) -> Unit,
) {
    // The glance fades in once loaded, and stays as the list scrolls: today stays in view, with
    // the room the menu button leaves.
    val glanceShown by animateFloatAsState(if (glance != null) 1f else 0f, appearTween(), label = "glance")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            // As tall as the more-menu button at its end (a 32 dp icon in 8 dp of padding), which
            // sits on its own at the same top: everything centers on that button.
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = glanceShown }
                .testTag("home_glance"),
        ) {
            if (glance != null) {
                HomeGlance(
                    glance = glance,
                    hideIncome = hideIncome,
                    firstFact = firstGlanceFact,
                )
            }
        }

        // As far from the month as the month is from the menu button.
        Spacer(Modifier.width(12.dp))

        NivyOutlinedButton(
            modifier = Modifier.horizontalSwipeListener(
                sensitivity = 75,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    onSelectNextMonth()
                },
                onSwipeRight = {
                    onSelectPreviousMonth()
                },
            ),
            iconStart = R.drawable.ic_calendar,
            text = period.toDisplayShort(
                startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            minWidth = 130.dp,
            onClear = backToThisMonth(period, onSelectPeriod),
            clearLabel = stringResource(R.string.month_this_month),
        ) {
            onShowMonthModal()
        }

        // The menu button's room, with the gap before it. As the list scrolls the button shrinks
        // and fades, as the pie chart's add button does, and this shrinks with it: the month
        // slides into the corner.
        if (percentExpanded > 0f) {
            Spacer(Modifier.width((12 + 48).dp * percentExpanded))
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun CashFlowInfo(
    currency: String,
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideBalance: Boolean,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    onBalanceClick: () -> Unit,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
    balanceLoaded: Boolean = true,
    statsLoaded: Boolean = true,
) {
    // Until loaded, the balance and the month's amounts keep their place, empty; then they fade in.
    val balanceShown = animateFloatAsState(if (balanceLoaded) 1f else 0f, appearTween(), label = "balance")
    val statsShown = animateFloatAsState(if (statsLoaded) 1f else 0f, appearTween(), label = "stats")

    Column(
        modifier = modifier,
    ) {
        BalanceRow(
            modifier = Modifier
                .graphicsLayer { alpha = balanceShown.value }
                .padding(horizontal = 24.dp)
                .clickableNoIndication(rememberInteractionSource()) {
                    if (hideBalance) {
                        onHiddenBalanceClick()
                    } else {
                        onBalanceClick()
                    }
                }
                .testTag("home_balance"),
            currency = currency,
            balance = balance,
            shortenBigNumbers = true,
            hiddenMode = hideBalance
        )

        Spacer(modifier = Modifier.height(24.dp))

        IncomeExpenses(
            currency = currency,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            hideIncome = hideIncome,
            onHiddenIncomeClick = onHiddenIncomeClick,
            amountsShown = statsShown,
        )

        val cashflow = monthlyIncome - monthlyExpenses
        val page = MaterialTheme.colorScheme.background
        // Hidden with the income too: with the expenses beside it, it would tell the income.
        if (cashflow != 0.0 && !hideBalance && !hideIncome) {
            // As far from the cards as from the line under the header: 4 here and the 16 above it.
            Spacer(Modifier.height(20.dp))

            Text(
                modifier = Modifier
                    .graphicsLayer { alpha = statsShown.value }
                    .padding(
                        start = 24.dp,
                    ),
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    // Readable on light pages; the text color in minimal colors.
                    color = if (cashflow < 0) Gray else incomeGreen(page).shown(),
                    fontWeight = FontWeight.Bold
                ),
            )

            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomeExpenses(
    currency: String,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    amountsShown: State<Float>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        val nav = navigation()

        HeaderCard(
            icon = R.drawable.ic_income,
            amountColor = incomeGreen(MaterialTheme.colorScheme.surfaceVariant),
            label = stringResource(R.string.income),
            currency = currency,
            amount = monthlyIncome,
            hidden = hideIncome,
            testTag = "home_card_income",
            amountShown = amountsShown,
        ) {
            if (hideIncome) {
                onHiddenIncomeClick()
            } else {
                nav.navigateTo(
                    PieChartStatisticScreen(
                        type = TransactionType.INCOME,
                    ),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        HeaderCard(
            icon = R.drawable.ic_expense,
            amountColor = expenseRed(MaterialTheme.colorScheme.surfaceVariant),
            label = stringResource(R.string.expenses),
            currency = currency,
            amount = monthlyExpenses.absoluteValue,
            testTag = "home_card_expense",
            amountShown = amountsShown,
        ) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = TransactionType.EXPENSE,
                ),
            )
        }

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun RowScope.HeaderCard(
    @DrawableRes icon: Int,
    amountColor: Color,
    label: String,
    currency: String,
    amount: Double,
    testTag: String,
    amountShown: State<Float>,
    hidden: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium)
            // Like the transaction cards: a plain card, the amount in green or red.
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag(testTag)
            .clickable(
                onClick = onClick,
            ),
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))

            NivyIcon(
                icon = icon,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                ),
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .graphicsLayer { alpha = amountShown.value },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(20.dp))

            if (hidden) {
                // Like the hidden balance: tapping shows the amount for a few seconds.
                Text(
                    text = HiddenAmount,
                    style = MaterialTheme.typography.numberTitleLarge.copy(
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            } else {
                AmountCurrencyB1(
                    amount = amount,
                    currency = currency,
                    textColor = amountColor,
                    shortenBigNumbers = true,
                )
            }

            Spacer(Modifier.width(4.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}
