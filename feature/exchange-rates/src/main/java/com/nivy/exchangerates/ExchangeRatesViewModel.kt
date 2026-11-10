package com.nivy.exchangerates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.nivy.base.Toaster
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.exchangerates.data.RateUi
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
@HiltViewModel
class ExchangeRatesViewModel @Inject constructor(
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val currencyRepo: CurrencyRepository,
    private val exchangeRatesRepo: ExchangeRatesRepository,
    private val dispatchers: DispatchersProvider,
    private val toaster: Toaster,
    private val userWrites: UserWrites,
) : ComposeViewModel<RatesState, RatesEvent>() {
    private var searchQuery by mutableStateOf("")
    private var baseCurrency by mutableStateOf<AssetCode?>(null)

    private fun toUi(exchangeRate: ExchangeRate): RateUi = RateUi(
        from = exchangeRate.baseCurrency.code,
        to = exchangeRate.currency.code,
        rate = exchangeRate.rate.value
    )

    @Composable
    override fun uiState(): RatesState {
        LaunchedEffect(Unit) {
            baseCurrency = currencyRepo.getBaseCurrency().also {
                viewModelScope.launch {
                    syncExchangeRatesUseCase.sync(it)
                        .onLeft { toaster.show(R.string.msg_exchange_rates_not_downloaded) }
                }
            }
        }

        val rates = getRates()

        return RatesState(
            baseCurrency = baseCurrency?.code ?: "",
            manual = rates.filter { it.manualOverride }.map(::toUi).toImmutableList(),
            automatic = rates.filter { !it.manualOverride }.map(::toUi).toImmutableList()
        )
    }

    @Composable
    private fun getRates(): List<ExchangeRate> {
        val rates by remember { exchangeRatesRepo.findAll() }
            .collectAsState(initial = emptyList())

        return rates.filter {
            if (searchQuery.isNotBlank()) {
                it.currency.code.contains(searchQuery, ignoreCase = true)
            } else {
                true
            }
        }.filter { baseCurrency == it.baseCurrency }
    }

    // region Event Handling
    override fun onEvent(event: RatesEvent) {
        viewModelScope.launch {
            when (event) {
                is RatesEvent.RemoveOverride -> handleRemoveOverride(event)
                is RatesEvent.Search -> handleSearch(event)
                is RatesEvent.UpdateRate -> handleUpdateRate(event)
                is RatesEvent.AddRate -> handleAddRate(event)
            }
        }
    }

    private suspend fun handleRemoveOverride(event: RatesEvent.RemoveOverride) {
        withContext(dispatchers.io) {
            either {
                AssetCode.from(event.rate.from).bind() to AssetCode.from(event.rate.to).bind()
            }.onRight { (from, to) ->
                val removed = userWrites.run(R.string.msg_rate_not_removed) {
                    exchangeRatesRepo.deleteByBaseCurrencyAndCurrency(baseCurrency = from, currency = to)
                }
                // Sync to fetch the real rate
                if (removed) {
                    baseCurrency?.let { base ->
                        syncExchangeRatesUseCase.sync(base)
                            .onLeft { toaster.show(R.string.msg_exchange_rates_not_downloaded) }
                    }
                }
            }.onLeft { toaster.show(R.string.msg_rate_not_removed) }
        }
    }

    private fun handleSearch(event: RatesEvent.Search) {
        searchQuery = event.query.trim()
    }

    private suspend fun handleUpdateRate(event: RatesEvent.UpdateRate) {
        saveManualRate(from = event.rate.from, to = event.rate.to, rate = event.newRate)
    }

    private suspend fun handleAddRate(event: RatesEvent.AddRate) {
        saveManualRate(from = event.rate.from, to = event.rate.to, rate = event.rate.rate)
    }

    // A rate entered by hand, kept over the downloaded one; the validators' own text isn't for people.
    private suspend fun saveManualRate(from: String, to: String, rate: Double) {
        withContext(dispatchers.io) {
            either {
                ExchangeRate(
                    baseCurrency = AssetCode.from(from).bind(),
                    currency = AssetCode.from(to).bind(),
                    rate = PositiveDouble.from(rate).bind(),
                    manualOverride = true
                )
            }.onRight {
                userWrites.run(R.string.msg_rate_not_saved) { exchangeRatesRepo.save(it) }
            }.onLeft { toaster.show(R.string.msg_rate_not_saved) }
        }
    }
    // endregion
}
