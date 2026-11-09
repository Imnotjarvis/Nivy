package com.nivy.balance

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nivy.balance.detail.MonthDetail
import com.nivy.balance.detail.MonthSections
import com.nivy.base.model.Theme
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.typography.numberLabelMedium
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.CloseButton
import com.nivy.wallet.ui.theme.components.NivyDividerLine
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.horizontalSwipeListener
import com.nivy.wallet.utils.rememberSwipeListenerState
import kotlin.math.abs

@Composable
fun BoxWithConstraintsScope.BalanceScreen() {
    val viewModel: BalanceViewModel = viewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
internal fun BoxWithConstraintsScope.UI(
    state: BalanceState,
    onEvent: (BalanceEvent) -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
) {
    var choosePeriodModal: ChoosePeriodModalData? by remember { mutableStateOf(null) }

    // The balances, then the month in detail under them: the screen scrolls.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        TopBar(
            period = state.period,
            onPreviousMonth = { onEvent(BalanceEvent.OnPreviousMonth) },
            onNextMonth = { onEvent(BalanceEvent.OnNextMonth) },
            onShowChoosePeriodModal = {
                choosePeriodModal = ChoosePeriodModalData(
                    period = state.period
                )
            }
        )

        Spacer(Modifier.height(24.dp))

        CurrentBalance(
            currency = state.baseCurrencyCode,
            currentBalance = state.currentBalance,
            detail = state.detail,
        )

        Spacer(Modifier.height(24.dp))

        NivyDividerLine(
            modifier = Modifier
                .padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))

        BalanceAfterPlannedPayments(
            currency = state.baseCurrencyCode,
            currentBalance = state.currentBalance,
            plannedPaymentsAmount = state.plannedPaymentsAmount,
            balanceAfterPlannedPayments = state.balanceAfterPlannedPayments
        )

        state.detail?.let { detail ->
            MonthSections(detail = detail, currency = state.baseCurrencyCode, month = state.period.month)
        }

        Spacer(Modifier.height(32.dp))
    }

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            choosePeriodModal = null
        }
    ) {
        onEvent(BalanceEvent.OnSetPeriod(it))
    }
}

// Close, and the month: a tap chooses another, a swipe goes to the next or the previous one.
@Composable
private fun TopBar(
    period: TimePeriod,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowChoosePeriodModal: () -> Unit,
) {
    val nav = navigation()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CloseButton { nav.back() }

        Spacer(Modifier.weight(1f))

        NivyOutlinedButton(
            modifier = Modifier
                .horizontalSwipeListener(
                    sensitivity = 75,
                    state = rememberSwipeListenerState(),
                    onSwipeLeft = onNextMonth,
                    onSwipeRight = onPreviousMonth,
                )
                .testTag("balance_month"),
            iconStart = R.drawable.ic_calendar,
            text = period.toDisplayShort(
                startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            onClick = onShowChoosePeriodModal,
        )
    }
}

@Composable
private fun ColumnScope.CurrentBalance(
    currency: String,
    currentBalance: Double,
    detail: MonthDetail?,
) {
    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(R.string.current_balance),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = Gray,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(4.dp))

    BalanceRow(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        currency = currency,
        balance = currentBalance
    )

    // How it moved since the month began, while it's the current month.
    val change = detail?.sinceStart
    val firstDay = detail?.days?.firstOrNull()
    if (change != null && firstDay != null) {
        Spacer(Modifier.height(4.dp))
        val page = MaterialTheme.colorScheme.background
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("balance_since_start"),
            text = stringResource(
                R.string.month_since,
                (if (change >= 0) "+" else "-") + abs(change).format(currency),
                currency,
                with(LocalTimeFormatter.current) {
                    firstDay.atStartOfDay().format(TimeFormatter.Style.DateOnly(includeWeekDay = false))
                },
            ),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (change >= 0) incomeGreen(page) else expenseRed(page),
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun ColumnScope.BalanceAfterPlannedPayments(
    currency: String,
    currentBalance: Double,
    plannedPaymentsAmount: Double,
    balanceAfterPlannedPayments: Double
) {
    Text(
        modifier = Modifier
            .padding(horizontal = 24.dp),
        text = stringResource(R.string.balance_after_payments),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = Orange,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        BalanceRow(
            currency = currency,
            balance = balanceAfterPlannedPayments,

            balanceFontSize = 30.sp,

        )

        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "${currentBalance.format(2)} $currency",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(Modifier.height(2.dp))

            val plusSign = if (plannedPaymentsAmount >= 0) "+" else ""
            Text(
                text = "${plusSign}${plannedPaymentsAmount.format(2)} $currency",
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    NivyPreview(theme) {
        UI(
            state = BalanceState(
                period = TimePeriod(month = Month.monthsList().first()),
                baseCurrencyCode = "BGN",
                currentBalance = 9326.55,
                balanceAfterPlannedPayments = 8426.0,
                plannedPaymentsAmount = -900.55,
            )
        )
    }
}
