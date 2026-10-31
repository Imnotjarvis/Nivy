package com.ivy.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.wallet.MainScreenState
import com.ivy.wallet.data.model.MainTab
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.wallet.utils.asLiveData
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.domain.logic.model.CreateAccountData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainScreenState: MainScreenState,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val accountCreator: AccountCreator,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val _currency = MutableLiveData<String>()
    val currency = _currency.asLiveData()

    fun start() {
        viewModelScope.launch {
            val baseCurrency = currencyRepository.getBaseCurrency()
            _currency.value = baseCurrency.code

            ioThread {
                // Sync exchange rates
                syncExchangeRatesUseCase.sync(baseCurrency)
            }
        }
    }

    fun selectTab(tab: MainTab) {
        mainScreenState.selectTab(tab)
    }

    fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            accountCreator.createAccount(data) {}
        }
    }
}
