package com.nivy.accounts

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.accounts.usecase.AccountDataUseCase
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.features.Features
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.AccountData
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.ioThread
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.LoadTimer
import com.nivy.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val periodState: PeriodState,
    private val appPreferences: AppPreferences,
    private val currencyRepository: CurrencyRepository,
    private val accountDataUseCase: AccountDataUseCase,
    private val accountRepository: AccountRepository,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<AccountsState, AccountsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var totalBalanceWithExcluded by mutableStateOf("")
    private var totalBalanceWithExcludedText by mutableStateOf("")
    private var totalBalanceWithoutExcluded by mutableStateOf("")
    private var totalBalanceWithoutExcludedText by mutableStateOf("")
    private var reorderVisible by mutableStateOf(false)
    private var typeFilter by mutableStateOf<AccountType?>(null)
    private var showArchived by mutableStateOf(false)
    private var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    is DataWriteEvent.AccountChange -> {
                        onStart()
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    @Composable
    override fun uiState(): AccountsState {
        LaunchedEffect(Unit) {
            onStart()
        }

        val unarchived = accountsData.filterNot { it.account.archived }
        val accountTypes = unarchived.map { it.account.type }.distinct().sorted()
        // A filter by a type no account has anymore would show nothing.
        val filter = typeFilter?.takeIf { it in accountTypes }
        return AccountsState(
            baseCurrency = baseCurrency,
            accountsData = accountsData.toImmutableList(),
            shownAccountsData = accountsData
                .filter { showArchived || !it.account.archived }
                .filter { filter == null || it.account.type == filter }
                .toImmutableList(),
            accountTypes = accountTypes.toImmutableList(),
            typeFilter = filter,
            archivedCount = accountsData.size - unarchived.size,
            showArchived = showArchived,
            totalBalanceWithExcluded = totalBalanceWithExcluded,
            totalBalanceWithExcludedText = totalBalanceWithExcludedText,
            totalBalanceWithoutExcluded = totalBalanceWithoutExcluded,
            totalBalanceWithoutExcludedText = totalBalanceWithoutExcludedText,
            reorderVisible = reorderVisible,
            compactAccountsModeEnabled = getCompactAccountsMode(),
            hideTotalBalance = getHideTotalBalance(),
            loaded = loaded,
        )
    }

    @Composable
    private fun getHideTotalBalance(): Boolean {
        return features.hideTotalBalance.asEnabledState()
    }

    @Composable
    private fun getCompactAccountsMode(): Boolean {
        return features.compactAccountsMode.asEnabledState()
    }

    override fun onEvent(event: AccountsEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is AccountsEvent.OnReorder -> reorder(event.reorderedList)
                is AccountsEvent.OnReorderModalVisible -> reorderModalVisible(event.reorderVisible)
                is AccountsEvent.OnTypeFilter -> typeFilter = event.type
                is AccountsEvent.OnShowArchived -> showArchived = event.show
            }
        }
    }

    private suspend fun reorder(newOrder: List<AccountData>) {
        ioThread {
            newOrder.mapIndexed { index, accountData ->
                accountRepository.save(accountData.account.copy(orderNum = index.toDouble()))
            }
        }

        startInternally()
    }

    private fun onStart() {
        viewModelScope.launch(Dispatchers.Default) {
            startInternally()
        }
    }

    private suspend fun startInternally() {
        val period = com.nivy.wallet.data.model.TimePeriod.currentMonth(
            startDayOfMonth = periodState.startDayOfMonth,
            timeProvider = timeProvider,
        ) // this must be monthly
        val range = period.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        val timer = LoadTimer("Accounts")
        val baseCurrencyCode = currencyRepository.getBaseCurrency().code
        val accounts = timer.step("accounts") { accountRepository.findAll().toImmutableList() }

        val includeTransfersInCalc = appPreferences.transfersAsIncomeExpense()

        val accountsDataList = timer.step("balances") {
            accountDataUseCase.accountsData(
                accounts = accounts,
                range = range.toCloseTimeRange(),
                baseCurrency = baseCurrencyCode,
                includeTransfersInCalc = includeTransfersInCalc
            )
        }

        val totalBalanceWithExcludedAccounts = accountsDataList.totalIn(baseCurrencyCode, withExcluded = true)
        val totalBalanceWithoutExcludedAccounts = accountsDataList.totalIn(baseCurrencyCode, withExcluded = false)

        baseCurrency = baseCurrencyCode
        accountsData = accountsDataList
        totalBalanceWithExcluded = totalBalanceWithExcludedAccounts.toString()
        totalBalanceWithExcludedText = context.getString(
            R.string.total,
            baseCurrencyCode,
            totalBalanceWithExcludedAccounts.format(
                baseCurrencyCode
            )
        )
        totalBalanceWithoutExcluded = totalBalanceWithoutExcludedAccounts.toString()
        totalBalanceWithoutExcludedText = context.getString(
            R.string.total_exclusive,
            baseCurrencyCode,
            totalBalanceWithoutExcludedAccounts.format(
                baseCurrencyCode
            )
        )
        loaded = true
        timer.done()
    }

    private fun reorderModalVisible(visible: Boolean) {
        reorderVisible = visible
    }
}
