package com.ivy.accounts

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.features.Features
import com.ivy.wallet.PeriodState
import com.ivy.wallet.data.model.AccountData
import com.ivy.wallet.data.model.toCloseTimeRange
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.LoadTimer
import com.ivy.ui.R
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
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
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val accountDataAct: AccountDataAct,
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
            hideTotalBalance = getHideTotalBalance()
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
        val period = com.ivy.wallet.data.model.TimePeriod.currentMonth(
            startDayOfMonth = periodState.startDayOfMonth,
            timeProvider = timeProvider,
        ) // this must be monthly
        val range = period.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        val timer = LoadTimer("Accounts")
        val baseCurrencyCode = baseCurrencyAct()
        val accounts = timer.step("accounts") { accountRepository.findAll().toImmutableList() }

        val includeTransfersInCalc = appPreferences.transfersAsIncomeExpense()

        val accountsDataList = timer.step("balances") {
            accountDataAct(
                AccountDataAct.Input(
                    accounts = accounts,
                    range = range.toCloseTimeRange(),
                    baseCurrency = baseCurrencyCode,
                    includeTransfersInCalc = includeTransfersInCalc
                )
            )
        }

        val (totalBalanceWithExcludedAccounts, totalBalanceWithoutExcludedAccounts) = timer.step("totals") {
            calcWalletBalanceAct(
                CalcWalletBalanceAct.Input(
                    baseCurrency = baseCurrencyCode,
                    withExcluded = true
                )
            ).toDouble() to calcWalletBalanceAct(
                CalcWalletBalanceAct.Input(
                    baseCurrency = baseCurrencyCode
                )
            ).toDouble()
        }

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
        timer.done()
    }

    private fun reorderModalVisible(visible: Boolean) {
        reorderVisible = visible
    }
}
