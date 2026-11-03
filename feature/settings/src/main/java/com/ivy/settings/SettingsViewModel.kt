package com.ivy.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.Theme
import com.ivy.base.model.next
import com.ivy.base.Toaster
import com.ivy.base.refreshWidget
import com.ivy.base.time.TimeProvider
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.BudgetRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.RootScreen
import com.ivy.domain.usecase.csv.ExportCsvUseCase
import com.ivy.domain.usecase.data.DeleteAllDataUseCase
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.ui.time.DateFormatState
import com.ivy.wallet.ThemeState
import com.ivy.wallet.utils.getISOFormattedDateTime
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.utils.uiThread
import com.ivy.navigation.Navigation
import com.ivy.navigation.OnboardingScreen
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import com.ivy.wallet.domain.action.global.UpdateStartDayOfMonthAct
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber
import kotlinx.coroutines.CancellationException
import com.ivy.ui.R

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val themeState: ThemeState,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val nav: Navigation,
    private val appPreferences: AppPreferences,
    private val backupDataUseCase: BackupDataUseCase,
    private val startDayOfMonthAct: StartDayOfMonthAct,
    private val updateStartDayOfMonthAct: UpdateStartDayOfMonthAct,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val budgetRepository: BudgetRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val exportCsvUseCase: ExportCsvUseCase,
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider,
    private val toaster: Toaster,
    private val dateFormatState: DateFormatState,
) : ComposeViewModel<SettingsState, SettingsEvent>() {

    private val currencyCode = mutableStateOf("")
    private val name = mutableStateOf("")
    private val currentTheme = mutableStateOf<Theme>(Theme.AUTO)
    private val lockApp = mutableStateOf(false)
    private val showNotifications = mutableStateOf(true)
    private val hideCurrentBalance = mutableStateOf(false)
    private val hideIncome = mutableStateOf(false)
    private val treatTransfersAsIncomeExpense = mutableStateOf(false)
    private val startDateOfMonth = mutableIntStateOf(1)
    private val progressState = mutableStateOf(false)

    @Composable
    override fun uiState(): SettingsState {
        LaunchedEffect(Unit) {
            onStart()
        }

        return SettingsState(
            currencyCode = currencyCode.value,
            name = name.value,
            currentTheme = currentTheme.value,
            lockApp = lockApp.value,
            showNotifications = showNotifications.value,
            hideCurrentBalance = hideCurrentBalance.value,
            treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense.value,
            startDateOfMonth = startDateOfMonth.intValue.toString(),
            dateFormat = dateFormatState.format,
            progressState = progressState.value,
            hideIncome = hideIncome.value,
        )
    }

    private suspend fun onStart() {
        initializeCurrency()
        initializeName()
        initializeCurrentTheme()
        initializeLockApp()
        initializeShowNotifications()
        initializeHideCurrentBalance()
        initializeHideIncome()
        initializeTransfersAsIncomeExpense()
        initializeStartDateOfMonth()
    }

    private suspend fun initializeCurrency() {
        currencyCode.value = currencyRepository.getBaseCurrency().code
    }

    private suspend fun initializeName() {
        name.value = appPreferences.settings()?.name.orEmpty()
    }

    private suspend fun initializeCurrentTheme() {
        currentTheme.value = themeState.theme
    }

    private suspend fun initializeLockApp() {
        lockApp.value = appPreferences.isAppLockEnabled()
    }

    private suspend fun initializeShowNotifications() {
        showNotifications.value = appPreferences.showNotifications()
    }

    private suspend fun initializeHideCurrentBalance() {
        hideCurrentBalance.value = appPreferences.hideCurrentBalance()
    }

    private suspend fun initializeHideIncome() {
        hideIncome.value = appPreferences.hideIncome()
    }

    private suspend fun initializeTransfersAsIncomeExpense() {
        treatTransfersAsIncomeExpense.value = appPreferences.transfersAsIncomeExpense()
    }

    private suspend fun initializeStartDateOfMonth() {
        startDateOfMonth.intValue = startDayOfMonthAct()
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetCurrency -> setCurrency(event.newCurrency)
            is SettingsEvent.SetName -> setName(event.newName)
            is SettingsEvent.ExportToCsv -> exportToCSV(event.rootScreen)
            is SettingsEvent.BackupData -> exportToZip(event.rootScreen)
            SettingsEvent.SwitchTheme -> switchTheme()
            is SettingsEvent.SetLockApp -> setLockApp(event.lockApp)
            is SettingsEvent.SetShowNotifications -> setShowNotifications(event.showNotifications)
            is SettingsEvent.SetHideCurrentBalance -> setHideCurrentBalance(
                event.hideCurrentBalance
            )

            is SettingsEvent.SetHideIncome -> setHideIncome(
                event.hideIncome
            )

            is SettingsEvent.SetTransfersAsIncomeExpense -> setTransfersAsIncomeExpense(
                event.treatTransfersAsIncomeExpense
            )

            is SettingsEvent.SetStartDateOfMonth -> setStartDateOfMonth(event.startDate)

            // Saved; the root view model updates the format the app's dates use.
            is SettingsEvent.SetDateFormat -> viewModelScope.launch { appPreferences.setDateFormat(event.format) }

            // Saved; the root view model updates the radius the app's shapes use.
            is SettingsEvent.SetCornerRadius -> viewModelScope.launch { appPreferences.setCornerRadius(event.dp) }

            SettingsEvent.DeleteAllUserData -> deleteAllUserData()
        }
    }

    private fun setCurrency(newCurrency: String) {
        currencyCode.value = newCurrency

        viewModelScope.launch {
            ioThread {
                // Before the switch: the stored rates are relative to the old base currency.
                AssetCode.from(newCurrency).onRight { convertBudgets(to = it) }
                appPreferences.setBaseCurrency(newCurrency)
                AssetCode.from(newCurrency).onRight {
                    syncExchangeRatesUseCase.sync(it)
                }
            }
        }
    }

    /**
     * Budgets have no currency of their own: they are in the base currency, so their amounts
     * follow it. A budget without a rate for the new currency keeps its amount.
     */
    private suspend fun convertBudgets(to: AssetCode) {
        val from = currencyRepository.getBaseCurrency()
        if (from == to) return
        val converted = budgetRepository.findAll().mapNotNull { budget ->
            exchangeUseCase.convert(PositiveValue(budget.amount, from), to = to).getOrNull()
                ?.let { budget.copy(amount = it.amount) }
        }
        budgetRepository.saveMany(converted)
    }

    private fun setName(newName: String) {
        name.value = newName

        viewModelScope.launch {
            appPreferences.setUserName(newName)
        }
    }

    private fun exportToCSV(rootScreen: RootScreen) {
        rootScreen.createNewFile(
            "IvyWalletExport_${
                timeProvider.localNow().getISOFormattedDateTime()
            }.csv"
        ) { fileUri ->
            viewModelScope.launch {
                exportCsvUseCase.exportToFile(
                    outputFile = fileUri
                )

                rootScreen.shareCSVFile(
                    fileUri = fileUri
                )
            }
        }
    }

    private fun exportToZip(rootScreen: RootScreen) {
        rootScreen.createNewFile(
            "IvyWalletBackup_${
                timeProvider.localNow().getISOFormattedDateTime()
            }.zip"
        ) { fileUri ->
            viewModelScope.launch(Dispatchers.IO) {
                progressState.value = true
                // The file provider can refuse the write, or the disk be full.
                val exported = try {
                    backupDataUseCase.exportToFile(zipFileUri = fileUri)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Backup export failed")
                    false
                }
                progressState.value = false

                if (exported) {
                    uiThread {
                        rootScreen.shareZipFile(
                            fileUri = fileUri
                        )
                    }
                } else {
                    toaster.show(R.string.msg_export_failed)
                }
            }
        }
    }

    private fun switchTheme() {
        viewModelScope.launch {
            val theme = themeState.theme.next()
            appPreferences.setTheme(theme)
            themeState.switchTheme(theme)
            currentTheme.value = theme
        }
    }

    private fun setLockApp(lock: Boolean) {
        lockApp.value = lock

        viewModelScope.launch {
            appPreferences.setAppLockEnabled(lock)
            context.refreshWidget(WalletBalanceWidgetReceiver::class.java)
        }
    }

    private fun setShowNotifications(notificationsShow: Boolean) {
        showNotifications.value = notificationsShow

        viewModelScope.launch {
            appPreferences.setShowNotifications(notificationsShow)
        }
    }

    private fun setHideCurrentBalance(hideBalance: Boolean) {
        hideCurrentBalance.value = hideBalance

        viewModelScope.launch {
            appPreferences.setHideCurrentBalance(hideBalance)
        }
    }

    private fun setHideIncome(isHideIncome: Boolean) {
        hideIncome.value = isHideIncome

        viewModelScope.launch {
            appPreferences.setHideIncome(isHideIncome)
        }
    }

    private fun setTransfersAsIncomeExpense(setTransfersAsIncomeExpense: Boolean) {
        treatTransfersAsIncomeExpense.value = setTransfersAsIncomeExpense

        viewModelScope.launch {
            appPreferences.setTransfersAsIncomeExpense(setTransfersAsIncomeExpense)
        }
    }

    private fun setStartDateOfMonth(startDate: Int) {
        viewModelScope.launch {
            updateStartDayOfMonthAct(startDate).onRight {
                startDateOfMonth.intValue = it
            }
        }
    }

    private fun deleteAllUserData() {
        viewModelScope.launch {
            logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            deleteAllDataUseCase()
            nav.resetBackStack()
            nav.navigateTo(OnboardingScreen)
        }
    }
}
