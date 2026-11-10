package com.nivy.onboarding.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.nivy.base.model.Theme
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.model.Account
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import com.nivy.data.repository.AccountRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.data.DeleteAllDataUseCase
import com.nivy.onboarding.StarterData
import com.nivy.ui.ComposeViewModel
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.data.model.Category
import com.nivy.data.repository.CategoryRepository
import com.nivy.wallet.data.model.AccountBalance
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.wallet.utils.ioThread
import com.nivy.navigation.Navigation
import com.nivy.navigation.OnboardingScreen
import com.nivy.onboarding.OnboardingDetailState
import com.nivy.onboarding.OnboardingEvent
import com.nivy.onboarding.OnboardingState
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.wallet.domain.logic.CategoryCreator
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.wallet.domain.logic.notification.TransactionReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val nav: Navigation,
    private val accountDao: AccountDao,
    private val appPreferences: AppPreferences,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val categoryCreator: CategoryCreator,
    private val categoryRepository: CategoryRepository,
    private val accountCreator: AccountCreator,

    private val accountRepository: AccountRepository,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,

    // Only OnboardingRouter stuff
    transactionReminderScheduler: TransactionReminderScheduler,
    starterData: StarterData,
    deleteAllDataUseCase: DeleteAllDataUseCase,
) : ComposeViewModel<OnboardingDetailState, OnboardingEvent>() {

    private val _state = mutableStateOf(OnboardingState.SPLASH)
    val state: State<OnboardingState> = _state

    private val _currency = mutableStateOf(NivyCurrency.getDefault())
    private val _accounts = mutableStateOf(listOf<AccountBalance>().toImmutableList())
    private val _accountSuggestions = mutableStateOf(listOf<CreateAccountData>().toImmutableList())
    private val _categories = mutableStateOf(listOf<Category>().toImmutableList())
    private val _categorySuggestions =
        mutableStateOf(listOf<CreateCategoryData>().toImmutableList())

    @Composable
    override fun uiState(): OnboardingDetailState {
        return OnboardingDetailState(
            currency = _currency.value,
            accounts = _accounts.value,
            accountSuggestions = _accountSuggestions.value,
            categories = _categories.value,
            categorySuggestions = _categorySuggestions.value
        )
    }

    private val router = OnboardingRouter(
        state = _state,
        accounts = _accounts,
        accountSuggestions = _accountSuggestions,
        categories = _categories,
        categorySuggestions = _categorySuggestions,

        nav = nav,
        accountDao = accountDao,
        appPreferences = appPreferences,
        categoryRepository = categoryRepository,
        starterData = starterData,
        transactionReminderScheduler = transactionReminderScheduler,
        deleteAllDataUseCase = deleteAllDataUseCase,
        syncExchangeRatesUseCase = syncExchangeRatesUseCase,
    )

    private var restartOnboarding: () -> Unit = {}

    fun onBack() {
        router.onBack(viewModelScope = viewModelScope, restartOnboarding = restartOnboarding)
    }

    fun start(screen: OnboardingScreen, isSystemDarkMode: Boolean) {
        viewModelScope.launch {
            initiateSettings(isSystemDarkMode)

            restartOnboarding = { start(screen, isSystemDarkMode) }

            router.splashNext()
        }
    }

    private suspend fun initiateSettings(isSystemDarkMode: Boolean) {
        val defaultCurrency = NivyCurrency.getDefault()
        _currency.value = savedCurrency() ?: defaultCurrency

        if (appPreferences.settings() == null) {
            appPreferences.saveSettings(
                AppSettings(
                    theme = if (isSystemDarkMode) Theme.DARK else Theme.LIGHT,
                    name = "",
                    baseCurrency = defaultCurrency.code,
                    // The savings goal: none until the user sets one, in Budgets.
                    bufferAmount = 0.0,
                )
            )
        }
    }

    /** The base currency already saved, e.g. by a restored backup. */
    private suspend fun savedCurrency(): NivyCurrency? =
        appPreferences.settings()?.baseCurrency?.let(NivyCurrency::fromCode)

    override fun onEvent(event: OnboardingEvent) {
        viewModelScope.launch {
            when (event) {
                is OnboardingEvent.CreateAccount -> createAccount(event.data)
                is OnboardingEvent.CreateCategory -> createCategory(event.data)
                is OnboardingEvent.EditAccount -> editAccount(event.account, event.newBalance)
                is OnboardingEvent.EditCategory -> editCategory(event.updatedCategory)
                OnboardingEvent.ImportSkip -> importSkip()
                OnboardingEvent.GetStarted -> getStarted()
                OnboardingEvent.OnAddAccountsDone -> onAddAccountsDone()
                OnboardingEvent.OnAddAccountsSkip -> onAddAccountsSkip()
                OnboardingEvent.OnAddCategoriesDone -> onAddCategoriesDone()
                OnboardingEvent.OnAddCategoriesSkip -> onAddCategoriesSkip()
                is OnboardingEvent.SetBaseCurrency -> setBaseCurrency(event.baseCurrency)
                OnboardingEvent.StartFresh -> startFresh()
                OnboardingEvent.StartImport -> startImport()
            }
        }
    }

    private suspend fun getStarted() {
        router.welcomeNext()
    }
    // Step 1 ---------------------------------------------------------------------------------------

    // Step 2 ---------------------------------------------------------------------------------------
    private fun startImport() {
        router.startImport()
    }

    fun importSkip() {
        router.importSkip()
    }

    fun importFinished(success: Boolean) {
        if (success) {
            // A restored backup brings its base currency: offer that rather than the device's.
            viewModelScope.launch {
                savedCurrency()?.let { _currency.value = it }
            }
        }
        router.importFinished(success)
    }

    private fun startFresh() {
        router.startFresh()
    }
    // Step 2 ---------------------------------------------------------------------------------------

    private suspend fun setBaseCurrency(baseCurrency: NivyCurrency) {
        updateBaseCurrency(baseCurrency)
        router.setBaseCurrencyNext(
            baseCurrency = baseCurrency,
            accountsWithBalance = { accountsWithBalance() }
        )
    }

    private suspend fun updateBaseCurrency(baseCurrency: NivyCurrency) {
        appPreferences.setBaseCurrency(baseCurrency.code)
        _currency.value = baseCurrency
    }

    // --------------------- Accounts ---------------------------------------------------------------
    private suspend fun editAccount(account: Account, newBalance: Double) {
        accountCreator.editAccount(account, newBalance, creditCards = null) {
            _accounts.value = accountsWithBalance()
        }
    }

    private suspend fun createAccount(data: CreateAccountData) {
        accountCreator.createAccount(data) {
            _accounts.value = accountsWithBalance()
        }
    }

    private suspend fun accountsWithBalance(): ImmutableList<AccountBalance> = ioThread {
        accountRepository.findAll()
            .map {
                AccountBalance(
                    account = it,
                    balance = accountBalanceUseCase.balance(account = it).balance.toDouble()
                )
            }.toImmutableList()
    }

    private suspend fun onAddAccountsDone() {
        router.accountsNext()
    }

    private suspend fun onAddAccountsSkip() {
        router.accountsSkip()
    }
    // --------------------- Accounts ---------------------------------------------------------------

    // ---------------------------- Categories ------------------------------------------------------
    private suspend fun editCategory(updatedCategory: Category) {
        categoryCreator.editCategory(updatedCategory) {
            _categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    private suspend fun createCategory(data: CreateCategoryData) {
        categoryCreator.createCategory(data) {
            _categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    private suspend fun onAddCategoriesDone() {
        router.categoriesNext(baseCurrency = _currency.value)
    }

    private suspend fun onAddCategoriesSkip() {
        router.categoriesSkip(baseCurrency = _currency.value)
    }
    // ---------------------------- Categories ------------------------------------------------------
}
