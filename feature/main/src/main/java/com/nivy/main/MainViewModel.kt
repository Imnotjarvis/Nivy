package com.nivy.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nivy.data.repository.CurrencyRepository
import com.nivy.wallet.MainScreenState
import com.nivy.wallet.data.model.MainTab
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.wallet.utils.asLiveData
import com.nivy.wallet.domain.logic.model.CreateAccountData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainScreenState: MainScreenState,
    private val accountCreator: AccountCreator,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val _currency = MutableLiveData<String>()
    val currency = _currency.asLiveData()

    fun start() {
        viewModelScope.launch {
            _currency.value = currencyRepository.getBaseCurrency().code
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
