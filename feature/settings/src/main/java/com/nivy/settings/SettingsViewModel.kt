package com.nivy.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.nivy.base.Toaster
import com.nivy.base.model.AppLockDelay
import com.nivy.base.model.Theme
import com.nivy.base.model.next
import com.nivy.base.time.TimeProvider
import com.nivy.data.backup.BackupDataUseCase
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.RootScreen
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.csv.ExportCsvUseCase
import com.nivy.domain.usecase.data.DeleteAllDataUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.navigation.Navigation
import com.nivy.navigation.OnboardingScreen
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.ui.time.DateFormatState
import com.nivy.wallet.ThemeState
import com.nivy.wallet.domain.usecase.StartDayOfMonthUseCase
import com.nivy.wallet.utils.getISOFormattedDateTime
import com.nivy.wallet.utils.ioThread
import com.nivy.wallet.utils.uiThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val themeState: ThemeState,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val nav: Navigation,
    private val appPreferences: AppPreferences,
    private val backupDataUseCase: BackupDataUseCase,
    private val startDayOfMonthUseCase: StartDayOfMonthUseCase,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val budgetRepository: BudgetRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val timeProvider: TimeProvider,
    private val toaster: Toaster,
    private val dateFormatState: DateFormatState,
    private val userWrites: UserWrites,
) : ComposeViewModel<SettingsState, SettingsEvent>() {

    private val currencyCode = mutableStateOf("")
    private val currentTheme = mutableStateOf<Theme>(Theme.AUTO)
    private val lockApp = mutableStateOf(false)
    private val lockDelay = mutableStateOf(AppLockDelay.Default)
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
            currentTheme = currentTheme.value,
            lockApp = lockApp.value,
            lockDelay = lockDelay.value,
            showNotifications = showNotifications.value,
            hideCurrentBalance = hideCurrentBalance.value,
            treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense.value,
            startDateOfMonth = startDateOfMonth.intValue,
            dateFormat = dateFormatState.format,
            progressState = progressState.value,
            hideIncome = hideIncome.value,
        )
    }

    private suspend fun onStart() {
        initializeCurrency()
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

    private suspend fun initializeCurrentTheme() {
        currentTheme.value = themeState.theme
    }

    private suspend fun initializeLockApp() {
        lockApp.value = appPreferences.isAppLockEnabled()
        lockDelay.value = appPreferences.appLockDelay.first()
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
        startDateOfMonth.intValue = startDayOfMonthUseCase.startDay()
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetCurrency -> setCurrency(event.newCurrency)
            is SettingsEvent.ExportToCsv -> exportToCSV(event.rootScreen)
            is SettingsEvent.BackupData -> exportToZip(event.rootScreen)
            SettingsEvent.SwitchTheme -> switchTheme()
            is SettingsEvent.SetLockApp -> setLockApp(event.lockApp)
            is SettingsEvent.SetLockDelay -> setLockDelay(event.delay)
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
            is SettingsEvent.SetMinimalColors -> viewModelScope.launch {
                appPreferences.setMinimalColors(event.minimal)
            }
            // Saved, out of minimal colors; the root view model updates the colors the app draws with.
            is SettingsEvent.SetColorPalette -> viewModelScope.launch {
                appPreferences.setColorPalette(event.palette)
                appPreferences.setMinimalColors(false)
            }

            SettingsEvent.DeleteAllUserData -> deleteAllUserData()
        }
    }

    private fun setCurrency(newCurrency: String) {
        val previous = currencyCode.value
        currencyCode.value = newCurrency

        viewModelScope.launch {
            val switched = ioThread {
                // Before the switch: the stored rates are relative to the old base currency.
                val converted = userWrites.run(R.string.msg_change_not_saved) {
                    AssetCode.from(newCurrency).onRight { convertBudgets(to = it) }
                }
                if (converted) {
                    appPreferences.setBaseCurrency(newCurrency)
                    AssetCode.from(newCurrency).onRight {
                        syncExchangeRatesUseCase.sync(it)
                            .onLeft { toaster.show(R.string.msg_exchange_rates_not_downloaded) }
                    }
                }
                converted
            }
            // The base currency stays as it was, and the screen shows it again.
            if (!switched) currencyCode.value = previous
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

    private fun exportToCSV(rootScreen: RootScreen) {
        rootScreen.createNewFile(
            "NivyExport_${
                timeProvider.localNow().getISOFormattedDateTime()
            }.csv"
        ) { fileUri ->
            viewModelScope.launch {
                exportCsvUseCase.exportToFile(outputFile = fileUri).fold(
                    ifLeft = { toaster.show(R.string.msg_export_failed) },
                    ifRight = { rootScreen.shareCSVFile(fileUri = fileUri) },
                )
            }
        }
    }

    private fun exportToZip(rootScreen: RootScreen) {
        rootScreen.createNewFile(
            "NivyBackup_${
                timeProvider.localNow().getISOFormattedDateTime()
            }.zip"
        ) { fileUri ->
            viewModelScope.launch(Dispatchers.IO) {
                progressState.value = true
                // The file provider can refuse the write, or the disk be full: whatever fails is
                // shown as the export having failed.
                @Suppress("TooGenericExceptionCaught")
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
        }
    }

    private fun setLockDelay(delay: AppLockDelay) {
        lockDelay.value = delay

        viewModelScope.launch {
            appPreferences.setAppLockDelay(delay)
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
            startDayOfMonthUseCase.setStartDay(startDate).onRight {
                startDateOfMonth.intValue = it
            }
        }
    }

    // Deletes everything and starts over, from onboarding; refused by the database, nothing is
    // deleted, and the app stays where it is.
    private fun deleteAllUserData() {
        viewModelScope.launch {
            if (userWrites.run(R.string.msg_change_not_saved) { deleteAllDataUseCase() }) {
                nav.resetBackStack()
                nav.navigateTo(OnboardingScreen)
            }
        }
    }
}
