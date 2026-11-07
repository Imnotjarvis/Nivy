package com.nivy.reports.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NonZeroDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.ui.ComposeViewModel
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
 * Income and expenses of the last months, of the accounts included in the balance, as Home counts
 * them; and the net worth at each month's end, of every account, as the Accounts tab counts it:
 * investment accounts at their value.
 */
@Stable
@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val walletBalanceUseCase: WalletBalanceUseCase,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
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
            val included = accountRepository.findAll().filter { it.includeInBalance }.map { it.id }.toSet()
            val transactions = transactionRepository.findAllBetween(ranges.first().from, ranges.last().to)
            val sums = withContext(dispatchers.default) { monthSums(transactions, included, ranges) }
            val now = timeProvider.utcNow()

            months = periods.indices.map { i ->
                MonthTrend(
                    period = periods[i],
                    income = sums[i].income.inCurrency(base),
                    expense = sums[i].expense.inCurrency(base),
                    // At the month's end, or now for the current month.
                    netWorth = walletBalanceUseCase.balance(
                        baseCurrency = base.code,
                        range = ClosedTimeRange.to(minOf(ranges[i].to, now)),
                        withExcluded = true,
                    ).toDouble(),
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
