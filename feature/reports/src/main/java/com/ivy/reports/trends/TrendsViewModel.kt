package com.ivy.reports.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NonZeroDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.data.model.toCloseTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A month of the trends, in the base currency. */
@Immutable
data class MonthTrend(
    val period: TimePeriod,
    val income: Double,
    val expense: Double,
    val netWorth: Double,
)

@Immutable
data class TrendsState(
    val baseCurrency: String,
    // Oldest first, the current month last.
    val months: ImmutableList<MonthTrend>,
    val selectedIndex: Int,
)

sealed interface TrendsEvent {
    data class SelectMonth(val index: Int) : TrendsEvent
}

/**
 * Income, expenses and net worth of the last months, counting the accounts included in the
 * balance, as the home screen does.
 */
@Stable
@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<TrendsState, TrendsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var months by mutableStateOf<ImmutableList<MonthTrend>>(persistentListOf())
    private var selectedIndex by mutableStateOf<Int?>(null)

    @Composable
    override fun uiState(): TrendsState {
        LaunchedEffect(Unit) {
            load()
        }

        return TrendsState(
            baseCurrency = baseCurrency,
            months = months,
            selectedIndex = selectedIndex ?: months.lastIndex,
        )
    }

    override fun onEvent(event: TrendsEvent) {
        when (event) {
            is TrendsEvent.SelectMonth -> selectedIndex = event.index
        }
    }

    private fun load() {
        viewModelScope.launch {
            val base = currencyRepository.getBaseCurrency()
            val startDay = appPreferences.startDayOfMonth()
            val current = TimePeriod.currentMonth(startDayOfMonth = startDay, timeProvider = timeProvider)
            val currentMonth = current.month ?: return@launch
            val currentYear = current.year ?: return@launch
            val periods = (MonthsShown - 1 downTo 0).map {
                currentMonth.incrementMonthPeriod(-it.toLong(), currentYear)
            }
            val ranges = periods.map {
                it.toRange(startDay, timeConverter, timeProvider).toCloseTimeRange()
            }
            val accounts = accountRepository.findAll().filter { it.includeInBalance }.map { it.id }.toSet()
            val transactions = transactionRepository.findAll()
            val sums = withContext(Dispatchers.Default) {
                monthSums(transactions, accounts, ranges, now = timeProvider.utcNow())
            }

            months = periods.zip(sums) { period, month ->
                MonthTrend(
                    period = period,
                    income = month.income.inCurrency(base),
                    expense = month.expense.inCurrency(base),
                    netWorth = month.netWorth.inCurrency(base),
                )
            }.toImmutableList()
            baseCurrency = base.code
        }
    }

    /** The amounts converted and summed; one without a rate doesn't count, as in the balance. */
    private suspend fun Map<AssetCode, Double>.inCurrency(currency: AssetCode): Double {
        val nonZero = mapNotNull { (asset, amount) ->
            NonZeroDouble.from(amount).getOrNull()?.let { asset to it }
        }.toMap()
        if (nonZero.isEmpty()) return 0.0
        return exchangeUseCase.convert(nonZero, to = currency).exchanged.getOrNull()?.amount?.value ?: 0.0
    }

    private companion object {
        const val MonthsShown = 12
    }
}
