package com.nivy.onboarding.viewmodel

import androidx.compose.runtime.MutableState
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.preferences.AppPreferences
import com.nivy.domain.usecase.data.DeleteAllDataUseCase
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.data.model.Category
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.CategoryRepository
import com.nivy.wallet.data.model.AccountBalance
import com.nivy.wallet.utils.ioThread
import com.nivy.navigation.ImportScreen
import com.nivy.navigation.MainScreen
import com.nivy.navigation.Navigation
import com.nivy.navigation.OnboardingScreen
import com.nivy.onboarding.OnboardingState
import com.nivy.onboarding.StarterData
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.wallet.domain.logic.notification.TransactionReminderScheduler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val SplashDuration = 1.seconds

class OnboardingRouter(
    private val state: MutableState<OnboardingState>,
    private val accounts: MutableState<ImmutableList<AccountBalance>>,
    private val accountSuggestions: MutableState<ImmutableList<CreateAccountData>>,
    private val categories: MutableState<ImmutableList<Category>>,
    private val categorySuggestions: MutableState<ImmutableList<CreateCategoryData>>,

    private val nav: Navigation,
    private val accountDao: AccountDao,
    private val appPreferences: AppPreferences,
    private val transactionReminderScheduler: TransactionReminderScheduler,
    private val starterData: StarterData,
    private val categoryRepository: CategoryRepository,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
) {

    // The user restored their data with an import, rather than starting fresh: the currency step
    // then finishes onboarding, and going back from it discards the import.
    private var restoredFromImport = false

    // Back goes one onboarding step back. The screen doesn't send it on the login step, where back
    // leaves the app, and it does nothing on the splash.
    fun onBack(
        viewModelScope: CoroutineScope,
        restartOnboarding: () -> Unit
    ) {
        when (state.value) {
            OnboardingState.SPLASH, OnboardingState.LOGIN -> {
                // nothing to go back to
            }

            OnboardingState.CHOOSE_PATH -> {
                state.value = OnboardingState.LOGIN
            }

            OnboardingState.CURRENCY -> {
                if (restoredFromImport) {
                    viewModelScope.launch {
                        deleteAllDataUseCase()
                        nav.resetBackStack()
                        nav.navigateTo(OnboardingScreen)
                        restoredFromImport = false
                        restartOnboarding()
                        state.value = OnboardingState.LOGIN
                    }
                } else {
                    // fresh user
                    state.value = OnboardingState.CHOOSE_PATH
                }
            }

            OnboardingState.ACCOUNTS -> {
                state.value = OnboardingState.CURRENCY
            }

            OnboardingState.CATEGORIES -> {
                state.value = OnboardingState.ACCOUNTS
            }
        }
    }

    // ------------------------------------- Step 0 - Splash ----------------------------------------
    suspend fun splashNext() {
        if (state.value == OnboardingState.SPLASH) {
            delay(SplashDuration)

            state.value = OnboardingState.LOGIN
        }
    }
    // ------------------------------------- Step 0 -------------------------------------------------

    // ------------------------------------- Step 1 - Login -----------------------------------------
    suspend fun offlineAccountNext() {
        state.value = OnboardingState.CHOOSE_PATH
    }
    // ------------------------------------- Step 1 -------------------------------------------------

    // ------------------------------------- Step 2 - Choose path -----------------------------------
    fun startImport() {
        nav.navigateTo(
            ImportScreen(
                launchedFromOnboarding = true
            )
        )
    }

    fun importSkip() {
        state.value = OnboardingState.CURRENCY
    }

    fun importFinished(success: Boolean) {
        if (success) {
            restoredFromImport = true
            state.value = OnboardingState.CURRENCY
        }
    }

    fun startFresh() {
        state.value = OnboardingState.CURRENCY
    }
    // ------------------------------------- Step 2 -------------------------------------------------

    // ------------------------------------- Step 3 - Currency --------------------------------------
    suspend fun setBaseCurrencyNext(
        baseCurrency: NivyCurrency,
        accountsWithBalance: suspend () -> ImmutableList<AccountBalance>,
    ) {
        routeToAccounts(
            baseCurrency = baseCurrency,
            accountsWithBalance = accountsWithBalance
        )

        if (restoredFromImport) {
            completeOnboarding(baseCurrency = baseCurrency)
        }
    }
    // ------------------------------------- Step 3 -------------------------------------------------

    // ------------------------------------- Step 4 - Accounts --------------------------------------
    suspend fun accountsNext() {
        routeToCategories()
    }

    suspend fun accountsSkip() {
        routeToCategories()

        // Skipping again after going back must not add Cash and Bank a second time.
        ioThread {
            if (accountDao.findAll().isEmpty()) starterData.preloadAccounts()
        }
    }
    // ------------------------------------- Step 4 -------------------------------------------------

    // ------------------------------------- Step 5 - Categories ------------------------------------
    suspend fun categoriesNext(baseCurrency: NivyCurrency?) {
        completeOnboarding(baseCurrency = baseCurrency)
    }

    suspend fun categoriesSkip(baseCurrency: NivyCurrency?) {
        // The starter categories first: completing shows Home, then waits for the exchange rates,
        // which offline takes the download's timeouts, and a process closed meanwhile would leave
        // a wallet with no categories and onboarding done.
        ioThread {
            starterData.preloadCategories()
        }

        completeOnboarding(baseCurrency = baseCurrency)
    }
    // ------------------------------------- Step 5 -------------------------------------------------

    // -------------------------------------- Routes ------------------------------------------------
    private suspend fun routeToAccounts(
        baseCurrency: NivyCurrency,
        accountsWithBalance: suspend () -> ImmutableList<AccountBalance>,
    ) {
        val accounts = accountsWithBalance()
        this.accounts.value = accounts

        accountSuggestions.value =
            starterData.accountSuggestions(baseCurrency.code)
        state.value = OnboardingState.ACCOUNTS
    }

    private suspend fun routeToCategories() {
        categories.value =
            ioThread { categoryRepository.findAll().toImmutableList() }
        categorySuggestions.value = starterData.categorySuggestions()

        state.value = OnboardingState.CATEGORIES
    }

    private suspend fun completeOnboarding(
        baseCurrency: NivyCurrency?
    ) {
        appPreferences.setOnboardingCompleted(true)

        navigateOutOfOnboarding()

        // the rest below is not UI stuff so I don't care
        ioThread {
            transactionReminderScheduler.scheduleReminder()

            AssetCode.from(baseCurrency?.code ?: NivyCurrency.getDefault().code)
                .onRight {
                    syncExchangeRatesUseCase.sync(baseCurrency = it)
                }
        }

        resetState()
    }

    private fun resetState() {
        state.value = OnboardingState.SPLASH
        restoredFromImport = false
    }

    private fun navigateOutOfOnboarding() {
        nav.resetBackStack()
        nav.navigateTo(MainScreen)
    }
    // -------------------------------------- Routes ------------------------------------------------
}
