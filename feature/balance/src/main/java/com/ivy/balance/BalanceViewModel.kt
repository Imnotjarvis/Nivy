package com.ivy.balance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.domain.usecase.planned.DueTransactionsUseCase
import com.ivy.wallet.PeriodState
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val periodState: PeriodState,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<BalanceState, BalanceEvent>() {

    private var period by mutableStateOf(periodState.selected)
    private var baseCurrencyCode by mutableStateOf("")
    private var currentBalance by mutableDoubleStateOf(0.0)
    private var plannedPaymentsAmount by mutableDoubleStateOf(0.0)
    private var balanceAfterPlannedPayments by mutableDoubleStateOf(0.0)
    private var numberOfMonthsAhead by mutableIntStateOf(1)

    @Composable
    override fun uiState(): BalanceState {
        LaunchedEffect(Unit) {
            start()
        }

        return BalanceState(
            period = period,
            balanceAfterPlannedPayments = balanceAfterPlannedPayments,
            currentBalance = currentBalance,
            baseCurrencyCode = baseCurrencyCode,
            plannedPaymentsAmount = plannedPaymentsAmount
        )
    }

    override fun onEvent(event: BalanceEvent) {
        when (event) {
            is BalanceEvent.OnNextMonth -> nextMonth()
            is BalanceEvent.OnSetPeriod -> setTimePeriod(event.timePeriod)
            is BalanceEvent.OnPreviousMonth -> previousMonth()
        }
    }

    private fun start(
        timePeriod: TimePeriod = periodState.selected
    ) {
        viewModelScope.launch {
            baseCurrencyCode = baseCurrencyAct()
            period = timePeriod

            currentBalance = calcWalletBalanceAct(
                CalcWalletBalanceAct.Input(baseCurrencyCode)
            ).toDouble()

            plannedPaymentsAmount = ioThread {
                val range = timePeriod.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)
                // + positive if Income > Expenses else - negative
                dueTransactionsUseCase.dueBalance(range.from(), range.to()) * if (numberOfMonthsAhead >= 0) {
                    numberOfMonthsAhead.toDouble()
                } else {
                    1.0
                }
            }
            balanceAfterPlannedPayments =
                currentBalance + plannedPaymentsAmount
        }
    }

    private fun setTimePeriod(timePeriod: TimePeriod) {
        start(timePeriod = timePeriod)
    }

    private fun nextMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        numberOfMonthsAhead += 1
        if (month != null) {
            start(
                timePeriod = periodState.selectMonth(month, 1L, year)
            )
        }
    }

    private fun previousMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        numberOfMonthsAhead -= 1
        if (month != null) {
            start(
                timePeriod = periodState.selectMonth(month, -1L, year)
            )
        }
    }
}
