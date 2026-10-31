package com.ivy.onboarding.viewmodel

import androidx.compose.runtime.MutableState
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.preferences.AppPreferences
import com.ivy.domain.usecase.data.DeleteAllDataUseCase
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.data.model.Category
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.CategoryRepository
import com.ivy.wallet.data.model.AccountBalance
import com.ivy.wallet.utils.ioThread
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.OnboardingScreen
import com.ivy.onboarding.OnboardingState
import com.ivy.onboarding.StarterData
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateCategoryData
import com.ivy.wallet.domain.logic.notification.TransactionReminderScheduler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    var isLoginCache = false

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
                if (isLoginCache) {
                    // user with Ivy account
                    viewModelScope.launch {
                        deleteAllDataUseCase()
                        nav.resetBackStack()
                        nav.navigateTo(OnboardingScreen)
                        isLoginCache = false
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
            delay(1000)

            state.value = OnboardingState.LOGIN
        }
    }
    // ------------------------------------- Step 0 -------------------------------------------------

    // ------------------------------------- Step 1 - Login -----------------------------------------
    private suspend fun isLogin(): Boolean {
        isLoginCache = ioThread { accountDao.findAll().isNotEmpty() }
        return isLoginCache
    }

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
            state.value = OnboardingState.CURRENCY
        }
    }

    fun startFresh() {
        state.value = OnboardingState.CURRENCY
    }
    // ------------------------------------- Step 2 -------------------------------------------------

    // ------------------------------------- Step 3 - Currency --------------------------------------
    suspend fun setBaseCurrencyNext(
        baseCurrency: IvyCurrency,
        accountsWithBalance: suspend () -> ImmutableList<AccountBalance>,
    ) {
        routeToAccounts(
            baseCurrency = baseCurrency,
            accountsWithBalance = accountsWithBalance
        )

        if (isLogin()) {
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

        ioThread {
            starterData.preloadAccounts()
        }
    }
    // ------------------------------------- Step 4 -------------------------------------------------

    // ------------------------------------- Step 5 - Categories ------------------------------------
    suspend fun categoriesNext(baseCurrency: IvyCurrency?) {
        completeOnboarding(baseCurrency = baseCurrency)
    }

    suspend fun categoriesSkip(baseCurrency: IvyCurrency?) {
        completeOnboarding(baseCurrency = baseCurrency)

        ioThread {
            starterData.preloadCategories()
        }
    }
    // ------------------------------------- Step 5 -------------------------------------------------

    // -------------------------------------- Routes ------------------------------------------------
    private suspend fun routeToAccounts(
        baseCurrency: IvyCurrency,
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
        baseCurrency: IvyCurrency?
    ) {
        appPreferences.setOnboardingCompleted(true)

        navigateOutOfOnboarding()

        // the rest below is not UI stuff so I don't care
        ioThread {
            transactionReminderScheduler.scheduleReminder()

            AssetCode.from(baseCurrency?.code ?: IvyCurrency.getDefault().code)
                .onRight {
                    syncExchangeRatesUseCase.sync(baseCurrency = it)
                }
        }

        resetState()
    }

    private fun resetState() {
        state.value = OnboardingState.SPLASH
    }

    private fun navigateOutOfOnboarding() {
        nav.resetBackStack()
        nav.navigateTo(MainScreen)
    }
    // -------------------------------------- Routes ------------------------------------------------
}
