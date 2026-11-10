package com.nivy.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import arrow.core.toOption
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.base.model.AccountType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountValue
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Category
import com.nivy.data.model.CreditCard
import com.nivy.data.model.Transaction
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.features.Features
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.account.AccountIncomeExpenseUseCase
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.wallet.domain.logic.AccountCreator
import com.nivy.navigation.Navigation
import com.nivy.navigation.TransactionsScreen
import com.nivy.ui.ComposeViewModel
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.logic.CategoryCreator
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// The transactions screen serves accounts and categories with their own stats and actions.
@Stable
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val periodState: PeriodState,
    private val accountLogic: AccountTransactionsLogic,
    private val categoryPeriodLoader: CategoryPeriodLoader,
    private val accountInvestmentData: AccountInvestmentData,
    private val accountCategoryDeletion: AccountCategoryDeletion,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val appPreferences: AppPreferences,
    private val accountTransactionsUseCase: AccountTransactionsUseCase,
    private val transactionsWithDateDividersUseCase: TransactionsWithDateDividersUseCase,
    private val currencyRepository: CurrencyRepository,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val accountIncomeExpenseUseCase: AccountIncomeExpenseUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val creditCardRepository: CreditCardRepository,
    private val accountValueRepository: AccountValueRepository,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val features: Features,
    private val dispatchers: DispatchersProvider,
    private val nav: Navigation,
) : ComposeViewModel<TransactionsState, TransactionsEvent>() {

    private val period = mutableStateOf(periodState.selected)
    private val categories = mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private val baseCurrency = mutableStateOf("")
    private val currency = mutableStateOf("")
    private val balance = mutableDoubleStateOf(0.0)
    private val balanceBaseCurrency = mutableStateOf<Double?>(null)
    private val income = mutableDoubleStateOf(0.0)
    private val expenses = mutableDoubleStateOf(0.0)

    // Upcoming
    private val upcoming = mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private val upcomingIncome = mutableDoubleStateOf(0.0)
    private val upcomingExpenses = mutableDoubleStateOf(0.0)
    private val upcomingExpanded = mutableStateOf(false)

    // Overdue
    private val overdue = mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private val overdueIncome = mutableDoubleStateOf(0.0)
    private val overdueExpenses = mutableDoubleStateOf(0.0)
    private val overdueExpanded = mutableStateOf(true)

    // History
    private val history =
        mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())

    private val account = mutableStateOf<Account?>(null)
    private val creditCards = mutableStateOf<ImmutableList<CreditCard>>(persistentListOf())
    private val category = mutableStateOf<Category?>(null)
    private val initWithTransactions = mutableStateOf(false)
    private val treatTransfersAsIncomeExpense = mutableStateOf(false)
    private val enableDeletionButton = mutableStateOf(false)
    private val skipAllModalVisible = mutableStateOf(false)
    private val deleteModal1Visible = mutableStateOf(false)
    private val choosePeriodModal = mutableStateOf<ChoosePeriodModalData?>(null)
    private val investment = mutableStateOf<InvestmentData?>(null)
    private val valuesExpanded = mutableStateOf(true)

    private val categoryLoadState = CategoryLoadState(
        period = period,
        baseCurrency = baseCurrency,
        accounts = accounts,
        category = category,
        initWithTransactions = initWithTransactions,
        balance = balance,
        income = income,
        expenses = expenses,
        history = history,
        upcoming = upcoming,
        upcomingIncome = upcomingIncome,
        upcomingExpenses = upcomingExpenses,
        overdue = overdue,
        overdueIncome = overdueIncome,
        overdueExpenses = overdueExpenses,
    )

    @Composable
    override fun uiState(): TransactionsState {
        return TransactionsState(
            period = period.value,
            baseCurrency = baseCurrency.value,
            currency = currency.value,
            categories = categories.value,
            accounts = accounts.value,
            account = account.value,
            creditCards = creditCards.value,
            category = category.value,
            balance = balance.doubleValue,
            balanceBaseCurrency = balanceBaseCurrency.value,
            income = income.doubleValue,
            expenses = expenses.doubleValue,
            initWithTransactions = initWithTransactions.value,
            treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense.value,
            history = history.value,
            upcoming = upcoming.value,
            upcomingExpanded = upcomingExpanded.value,
            upcomingIncome = upcomingIncome.doubleValue,
            upcomingExpenses = upcomingExpenses.doubleValue,
            overdue = overdue.value,
            overdueExpanded = overdueExpanded.value,
            overdueIncome = overdueIncome.doubleValue,
            overdueExpenses = overdueExpenses.doubleValue,
            enableDeletionButton = enableDeletionButton.value,
            skipAllModalVisible = skipAllModalVisible.value,
            deleteModal1Visible = deleteModal1Visible.value,
            choosePeriodModal = choosePeriodModal.value,
            showAccountColorsInTransactions = getShouldShowAccountSpecificColorInTransactions(),
            compactTransactions = features.compactTransactionsMode.asEnabledState(),
            investment = investment.value?.copy(valuesExpanded = valuesExpanded.value),
        )
    }

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
    }

    override fun onEvent(event: TransactionsEvent) {
        when (event) {
            is TransactionsEvent.Delete -> delete(event.screen)
            is TransactionsEvent.EditAccount -> editAccount(
                event.screen,
                event.account,
                event.newBalance,
                event.creditCards,
            )

            is TransactionsEvent.EditCategory -> editCategory(event.updatedCategory)
            is TransactionsEvent.NextMonth -> nextMonth(event.screen)
            is TransactionsEvent.PayOrGet -> payOrGet(event.screen, event.transaction)
            is TransactionsEvent.PreviousMonth -> previousMonth(event.screen)
            is TransactionsEvent.SetPeriod -> setPeriod(event.screen, event.period)
            is TransactionsEvent.SkipTransaction -> skipTransaction(event.screen, event.transaction)
            is TransactionsEvent.SkipTransactions -> skipTransactions(
                event.screen,
                event.transactions
            )

            is TransactionsEvent.UpdateAccountDeletionState -> updateAccountDeletionState(
                event.confirmationText
            )

            is TransactionsEvent.SetOverdueExpanded -> setOverdueExpanded(event.expanded)
            is TransactionsEvent.SetUpcomingExpanded -> setUpcomingExpanded(event.expanded)
            is TransactionsEvent.SetValuesExpanded -> valuesExpanded.value = event.expanded
            is TransactionsEvent.SaveValue -> saveValue(event.screen, event.value)
            is TransactionsEvent.DeleteValue -> deleteValue(event.screen, event.value)
            is TransactionsEvent.SetSkipAllModalVisible -> setSkipAllModalVisible(event.visible)
            is TransactionsEvent.OnDeleteModal1Visible -> setDeleteModal1Visible(event.delete)
            is TransactionsEvent.OnChoosePeriodModalData -> setChoosePeriodModalData(event.data)
        }
    }

    private suspend fun initForAccount(initialAccount: Account) {
        account.value = initialAccount
        creditCards.value = creditCardRepository.findByAccount(initialAccount.id).toImmutableList()
        val range = period.value.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        currency.value = initialAccount.asset.code

        val balanceOutput = accountBalanceUseCase.balance(
            account = initialAccount
        )
        val balanceValue = balanceOutput.balance.toDouble()
        balance.doubleValue = balanceValue
        investment.value = if (initialAccount.type == AccountType.INVESTMENT) {
            accountInvestmentData.investmentData(
                initialAccount,
                balanceValue,
                balanceOutput.gain?.toDouble(),
                balanceOutput.cash?.toDouble(),
                valuesExpanded.value,
            )
        } else {
            null
        }
        if (baseCurrency.value != currency.value) {
            balanceBaseCurrency.value = exchangeUseCase.exchange(
                data = ExchangeData(
                    baseCurrency = baseCurrency.value,
                    fromCurrency = currency.value.toOption()
                ),
                amount = balanceValue.toBigDecimal()
            ).getOrNull()?.toDouble()
        }

        val includeTransfersInCalc = appPreferences.transfersAsIncomeExpense()

        val incomeExpensePair = accountIncomeExpenseUseCase.incomeExpense(
            account = initialAccount,
            range = range.toCloseTimeRange(),
            includeTransfersInCalc = includeTransfersInCalc
        ).incomeExpensePair
        income.doubleValue = incomeExpensePair.income.toDouble()
        expenses.doubleValue = incomeExpensePair.expense.toDouble()

        val accountTransactions = accountTransactionsUseCase.transactions(
            accountId = initialAccount.id.value,
            range = range.toCloseTimeRange()
        )
        history.value = transactionsWithDateDividersUseCase.withDateDividers(
            baseCurrency = baseCurrency.value,
            transactions = accountTransactions.filter { it.settled }
        ).toImmutableList()

        // Upcoming
        upcomingIncome.doubleValue = withContext(dispatchers.io) {
            accountLogic.calculateUpcomingIncome(initialAccount, range)
        }

        upcomingExpenses.doubleValue = withContext(dispatchers.io) {
            accountLogic.calculateUpcomingExpenses(initialAccount, range)
        }

        upcoming.value = withContext(dispatchers.io) {
            accountLogic.upcoming(initialAccount, range).toImmutableList()
        }

        // Overdue
        overdueIncome.doubleValue = withContext(dispatchers.io) {
            accountLogic.calculateOverdueIncome(initialAccount, range)
        }

        overdueExpenses.doubleValue = withContext(dispatchers.io) {
            accountLogic.calculateOverdueExpenses(initialAccount, range)
        }

        overdue.value = withContext(dispatchers.io) {
            accountLogic.overdue(initialAccount, range).toImmutableList()
        }
    }

    /**
     * The page's account or category is gone: deleted on a page opened from this one, which went
     * back here. There's nothing to show, so this page goes back too.
     */
    private fun backFromGonePage() {
        nav.back()
    }

    private fun reset() {
        account.value = null
        category.value = null
        investment.value = null
    }

    private fun setUpcomingExpanded(expanded: Boolean) {
        upcomingExpanded.value = expanded
    }

    private fun setOverdueExpanded(expanded: Boolean) {
        overdueExpanded.value = expanded
    }

    private fun setPeriod(
        screen: TransactionsScreen,
        period: TimePeriod,
    ) {
        start(
            screen = screen,
            timePeriod = period,
            reset = false
        )
    }

    private fun setSkipAllModalVisible(visible: Boolean) {
        skipAllModalVisible.value = visible
    }

    private fun nextMonth(screen: TransactionsScreen) {
        val month = period.value.month
        val year = period.value.year ?: timeProvider.localDateNow().year
        if (month != null) {
            start(
                screen = screen,
                timePeriod = periodState.selectMonth(month, 1L, year),
                reset = false
            )
        }
    }

    private fun previousMonth(screen: TransactionsScreen) {
        val month = period.value.month
        val year = period.value.year ?: timeProvider.localDateNow().year
        if (month != null) {
            start(
                screen = screen,
                timePeriod = periodState.selectMonth(month, -1L, year),
                reset = false
            )
        }
    }

    private fun delete(screen: TransactionsScreen) {
        viewModelScope.launch {
            when {
                screen.accountId != null -> {
                    accountCategoryDeletion.deleteAccount(screen.accountId!!)
                }

                screen.categoryId != null -> {
                    accountCategoryDeletion.deleteCategory(screen.categoryId!!)
                }
            }
        }
    }

    private fun setDeleteModal1Visible(delete: Boolean) {
        deleteModal1Visible.value = delete
    }

    private fun setChoosePeriodModalData(data: ChoosePeriodModalData?) {
        choosePeriodModal.value = data
    }

    private fun editCategory(updatedCategory: Category) {
        viewModelScope.launch {
            categoryCreator.editCategory(updatedCategory) {
                category.value = it
            }
        }
    }

    private fun editAccount(
        screen: TransactionsScreen,
        account: Account,
        newBalance: Double,
        creditCards: List<CreditCard>?,
    ) {
        viewModelScope.launch {
            accountCreator.editAccount(account, newBalance, creditCards) {
                start(
                    screen = screen,
                    timePeriod = period.value,
                    reset = false
                )
            }
        }
    }

    private fun saveValue(screen: TransactionsScreen, value: AccountValue) {
        viewModelScope.launch {
            accountValueRepository.save(value)
            start(screen = screen, timePeriod = period.value, reset = false)
        }
    }

    private fun deleteValue(screen: TransactionsScreen, value: AccountValue) {
        viewModelScope.launch {
            accountValueRepository.deleteById(value.id)
            start(screen = screen, timePeriod = period.value, reset = false)
        }
    }

    private fun payOrGet(screen: TransactionsScreen, transaction: Transaction) {
        viewModelScope.launch {
            dueTransactionsUseCase.pay(transaction)
            start(
                screen = screen,
                reset = false
            )
        }
    }

    private fun skipTransaction(screen: TransactionsScreen, transaction: Transaction) {
        skipTransactions(screen, listOf(transaction))
    }

    private fun skipTransactions(screen: TransactionsScreen, transactions: List<Transaction>) {
        viewModelScope.launch {
            dueTransactionsUseCase.skip(transactions)
            start(
                screen = screen,
                reset = false
            )
        }
    }

    private fun updateAccountDeletionState(confirmationText: String) {
        enableDeletionButton.value = account.value?.name?.value == confirmationText ||
                category.value?.name?.value == confirmationText
    }

    private var loadJob: Job? = null

    fun start(
        screen: TransactionsScreen,
        timePeriod: TimePeriod? = periodState.selected,
        reset: Boolean = true,
    ) {
        if (reset) {
            reset()
        }

        // A newer load replaces one still running, which would otherwise put back what it read:
        // a month chosen after another would show the other one's figures.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            period.value = timePeriod ?: periodState.selected

            val baseCurrencyValue = currencyRepository.getBaseCurrency().code
            baseCurrency.value = baseCurrencyValue
            currency.value = baseCurrency.value

            categories.value = categoryRepository.findAll().toImmutableList()
            accounts.value = accountRepository.findAll().toImmutableList()
            initWithTransactions.value = false
            treatTransfersAsIncomeExpense.value = appPreferences.transfersAsIncomeExpense()

            when {
                screen.accountId != null -> {
                    val pageAccount = accountRepository.findById(AccountId(screen.accountId!!))
                        ?: return@launch backFromGonePage()
                    initForAccount(pageAccount)
                }

                screen.categoryId != null && screen.transactions.isEmpty() -> {
                    val pageCategory = categoryRepository.findById(CategoryId(screen.categoryId!!))
                        ?: return@launch backFromGonePage()
                    categoryPeriodLoader.initForCategory(
                        categoryLoadState,
                        pageCategory,
                        screen.accountIdFilterList
                    )
                }
                // unspecifiedCategory==false is explicitly checked to accommodate for a temp
                // AccountTransfers Category during Reports Screen
                screen.categoryId != null && screen.transactions.isNotEmpty() &&
                        screen.unspecifiedCategory == false -> {
                    val pageCategory = categoryRepository.findById(CategoryId(screen.categoryId!!))
                        ?: return@launch backFromGonePage()
                    categoryPeriodLoader.initForCategoryWithTransactions(
                        categoryLoadState,
                        pageCategory,
                        screen.accountIdFilterList,
                        screen.transactions
                    )
                }

                screen.unspecifiedCategory == true && screen.categoryId != null &&
                        screen.transactions.isNotEmpty() -> {
                    categoryPeriodLoader.initForAccountTransfersCategory(
                        categoryLoadState,
                        screen.accountIdFilterList,
                        screen.transactions
                    )
                }

                screen.unspecifiedCategory == true && screen.transactions.isNotEmpty() -> {
                    categoryPeriodLoader.initForUnspecifiedWithTransactions(
                        categoryLoadState,
                        screen.accountIdFilterList,
                        screen.transactions
                    )
                }

                screen.unspecifiedCategory == true -> {
                    categoryPeriodLoader.initForUnspecifiedCategory(categoryLoadState)
                }

                else -> error("no id provided")
            }
        }
    }
}
