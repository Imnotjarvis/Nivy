package com.nivy.balance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.balance.detail.MonthDetail
import com.nivy.balance.detail.MonthDetailUseCase
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.wallet.PeriodState
import com.nivy.ui.ComposeViewModel
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.utils.ioThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@Stable
@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val periodState: PeriodState,
    private val currencyRepository: CurrencyRepository,
    private val walletBalanceUseCase: WalletBalanceUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val monthDetailUseCase: MonthDetailUseCase,
) : ComposeViewModel<BalanceState, BalanceEvent>() {

    private var period by mutableStateOf(periodState.selected)
    private var baseCurrencyCode by mutableStateOf("")
    private var currentBalance by mutableDoubleStateOf(0.0)
    private var plannedPaymentsAmount by mutableDoubleStateOf(0.0)
    private var balanceAfterPlannedPayments by mutableDoubleStateOf(0.0)
    private var detail by mutableStateOf<MonthDetail?>(null)
    private var loadJob: Job? = null

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
            plannedPaymentsAmount = plannedPaymentsAmount,
            detail = detail,
        )
    }

    override fun onEvent(event: BalanceEvent) {
        when (event) {
            is BalanceEvent.OnNextMonth -> nextMonth()
            is BalanceEvent.OnSetPeriod -> setTimePeriod(event.timePeriod)
            is BalanceEvent.OnPreviousMonth -> previousMonth()
        }
    }

    // A newer load replaces one still running, which would otherwise put back an older month.
    private fun start(
        timePeriod: TimePeriod = periodState.selected
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            baseCurrencyCode = currencyRepository.getBaseCurrency().code
            period = timePeriod

            currentBalance = walletBalanceUseCase.balance(baseCurrencyCode).toDouble()

            val startDay = periodState.startDayOfMonth
            val end = timePeriod.toRange(startDay, timeConverter, timeProvider).to()
            plannedPaymentsAmount = ioThread {
                // What's due from the start of the current month to the end of the selected one:
                // + positive if Income > Expenses else - negative
                val currentMonth = TimePeriod.currentMonth(startDay, timeProvider)
                    .toRange(startDay, timeConverter, timeProvider)
                dueTransactionsUseCase.dueBalance(currentMonth.from(), end)
            } + chargesToCome(end)
            balanceAfterPlannedPayments =
                currentBalance + plannedPaymentsAmount

            // The month's detail takes longer: the balances come first, then it below them.
            detail = null
            detail = ioThread {
                monthDetailUseCase.detail(timePeriod, startDay, baseCurrencyCode)
            }
        }
    }

    /**
     * What's paid but dated after now, up to [end]: a credit card's purchases, until the card
     * charges them. The current balance leaves them out; the balance at the period's end doesn't.
     */
    private suspend fun chargesToCome(end: Instant): Double {
        if (!end.isAfter(timeProvider.utcNow())) return 0.0
        val atEnd = walletBalanceUseCase.balance(baseCurrencyCode, range = ClosedTimeRange.to(end))
        return atEnd.toDouble() - currentBalance
    }

    private fun setTimePeriod(timePeriod: TimePeriod) {
        start(timePeriod = timePeriod)
    }

    private fun nextMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        if (month != null) {
            start(
                timePeriod = periodState.selectMonth(month, 1L, year)
            )
        }
    }

    private fun previousMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        if (month != null) {
            start(
                timePeriod = periodState.selectMonth(month, -1L, year)
            )
        }
    }
}
