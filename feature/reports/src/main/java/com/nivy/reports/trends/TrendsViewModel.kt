package com.nivy.reports.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.CurrencyRepository
import com.nivy.reports.usecase.TrendsUseCase
import com.nivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TrendsState(
    val baseCurrency: String,
    val range: TrendsRange,
    // Once loaded.
    val detail: TrendsDetail?,
    // The month the charts mark, of the detail's months.
    val selectedIndex: Int,
)

sealed interface TrendsEvent {
    data class SelectMonth(val index: Int) : TrendsEvent
    data class SelectRange(val range: TrendsRange) : TrendsEvent
}

/** The trends of the last months: a year at first, or the range chosen. */
@Stable
@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val trendsUseCase: TrendsUseCase,
    private val currencyRepository: CurrencyRepository,
    private val appPreferences: AppPreferences,
) : ComposeViewModel<TrendsState, TrendsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var range by mutableStateOf(TrendsRange.Year)
    private var detail by mutableStateOf<TrendsDetail?>(null)
    private var selectedIndex by mutableStateOf<Int?>(null)
    private var loadJob: Job? = null

    @Composable
    override fun uiState(): TrendsState {
        LaunchedEffect(Unit) {
            load()
        }

        return TrendsState(
            baseCurrency = baseCurrency,
            range = range,
            detail = detail,
            selectedIndex = selectedIndex ?: detail?.months?.lastIndex ?: 0,
        )
    }

    override fun onEvent(event: TrendsEvent) {
        when (event) {
            is TrendsEvent.SelectMonth -> selectedIndex = event.index
            is TrendsEvent.SelectRange -> if (event.range != range) {
                range = event.range
                load()
            }
        }
    }

    // A newer load replaces one still running, which would otherwise put back another range.
    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val base = currencyRepository.getBaseCurrency()
            val loaded = trendsUseCase.trends(range, appPreferences.startDayOfMonth(), base)
            // The current month is marked again, among the range's months.
            selectedIndex = null
            detail = loaded
            baseCurrency = base.code
        }
    }
}
