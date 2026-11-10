package com.nivy.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.accounts.usecase.AccountDataUseCase
import com.nivy.base.model.AccountType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.features.Features
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.LoadTimer
import com.nivy.ui.R
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.AccountData
import com.nivy.wallet.data.model.toCloseTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val periodState: PeriodState,
    private val appPreferences: AppPreferences,
    private val currencyRepository: CurrencyRepository,
    private val accountDataUseCase: AccountDataUseCase,
    private val accountRepository: AccountRepository,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
    private val userWrites: UserWrites,
) : ComposeViewModel<AccountsState, AccountsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var totalBalanceWithExcluded by mutableStateOf("")
    private var totalBalanceWithoutExcluded by mutableStateOf("")
    private var reorderVisible by mutableStateOf(false)
    private var typeFilter by mutableStateOf<AccountType?>(null)
    private var showArchived by mutableStateOf(false)
    private var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    DataWriteEvent.AccountsChanged, DataWriteEvent.AllDataChange -> onStart()
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
            totalBalanceWithoutExcluded = totalBalanceWithoutExcluded,
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
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is AccountsEvent.OnReorder -> reorder(event.reorderedList)
                is AccountsEvent.OnReorderModalVisible -> reorderModalVisible(event.reorderVisible)
                is AccountsEvent.OnTypeFilter -> typeFilter = event.type
                is AccountsEvent.OnShowArchived -> showArchived = event.show
            }
        }
    }

    // One write: the change it reports reloads the list once, not once per account.
    private suspend fun reorder(newOrder: List<AccountData>) {
        userWrites.run(R.string.msg_change_not_saved) {
            accountRepository.saveMany(
                newOrder.mapIndexed { index, accountData -> accountData.account.copy(orderNum = index.toDouble()) }
            )
        }
    }

    private var loadJob: Job? = null

    // A newer load replaces one still running, which would otherwise put back what it read.
    private fun onStart() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatchers.default) {
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
        totalBalanceWithoutExcluded = totalBalanceWithoutExcludedAccounts.toString()
        loaded = true
        timer.done()
    }

    private fun reorderModalVisible(visible: Boolean) {
        reorderVisible = visible
    }
}
