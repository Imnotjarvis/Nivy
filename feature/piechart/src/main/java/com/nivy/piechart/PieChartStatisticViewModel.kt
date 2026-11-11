package com.nivy.piechart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Transaction
import com.nivy.data.model.expensesIn
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.utils.ioThread
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.navigation.BudgetPage
import com.nivy.data.repository.CategoryRepository
import com.nivy.domain.features.Features
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.piechart.usecase.PieChartUseCase
import com.nivy.ui.ComposeViewModel
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class PieChartStatisticViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val periodState: PeriodState,
    private val pieChartUseCase: PieChartUseCase,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionsWithDateDividersUseCase: TransactionsWithDateDividersUseCase,
    private val features: Features,
    private val transactionRepository: TransactionRepository,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<PieChartStatisticState, PieChartStatisticEvent>() {

    private var treatTransfersAsIncomeExpense by mutableStateOf(false)
    private var transactionType by mutableStateOf(TransactionType.INCOME)
    private var period by mutableStateOf(TimePeriod())
    private var baseCurrency by mutableStateOf("")
    private var totalAmount by mutableDoubleStateOf(0.0)
    private var categoryAmounts by mutableStateOf<ImmutableList<CategoryAmount>>(persistentListOf())
    private var selectedCategory by mutableStateOf<SelectedCategory?>(null)
    private var accountIdFilterList by mutableStateOf<ImmutableList<UUID>>(persistentListOf())
    private var showCloseButtonOnly by mutableStateOf(false)
    // What the chart is given to draw, rather than a period's transactions: a budget's expenses.
    private var transactions: ImmutableList<Transaction> = persistentListOf()
    private var choosePeriodModal by mutableStateOf<ChoosePeriodModalData?>(null)
    private var budgetPage by mutableStateOf<BudgetPage?>(null)
    private var history by mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
    private var baseData by mutableStateOf(AppBaseData("", persistentListOf(), persistentListOf()))
    private var breakdown by mutableStateOf(Breakdown.Categories)
    private var showBreakdownToggle by mutableStateOf(false)
    private var accountAmounts by mutableStateOf<ImmutableList<AccountAmount>>(persistentListOf())
    private var selectedAccount by mutableStateOf<Account?>(null)

    @Composable
    override fun uiState(): PieChartStatisticState {
        return PieChartStatisticState(
            transactionType = transactionType,
            period = period,
            baseCurrency = baseCurrency,
            totalAmount = totalAmount,
            categoryAmounts = categoryAmounts,
            selectedCategory = selectedCategory,
            accountIdFilterList = accountIdFilterList,
            showCloseButtonOnly = showCloseButtonOnly,
            choosePeriodModal = choosePeriodModal,
            budgetPage = budgetPage,
            history = history,
            baseData = baseData,
            compactTransactions = features.compactTransactionsMode.asEnabledState(),
            showAccountColors = features.showAccountColorsInTransactions.asEnabledState(),
            // By account only where there's more than one to split by.
            breakdown = if (showBreakdownToggle) breakdown else Breakdown.Categories,
            showBreakdownToggle = showBreakdownToggle,
            accountAmounts = accountAmounts,
            selectedAccount = selectedAccount,
        )
    }

    override fun onEvent(event: PieChartStatisticEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is PieChartStatisticEvent.OnSelectNextMonth -> nextMonth()
                is PieChartStatisticEvent.OnSelectPreviousMonth -> previousMonth()
                is PieChartStatisticEvent.OnSetPeriod -> onSetPeriod(event.timePeriod)
                is PieChartStatisticEvent.OnShowMonthModal -> configureMonthModal(event.timePeriod)
                is PieChartStatisticEvent.OnCategoryClicked -> onCategoryClicked(event.selection)
                is PieChartStatisticEvent.OnBreakdownSelected -> onBreakdownSelected(event.breakdown)
                is PieChartStatisticEvent.OnAccountClicked -> onAccountClicked(event.account)
                is PieChartStatisticEvent.OnStart -> start(event.screen)
            }
        }
    }

    private var loadJob: Job? = null

    // A newer load replaces one still running, which would otherwise put back what it read: a month
    // chosen after another would turn back into the other one when its load ended last.
    private fun loadLatest(load: suspend () -> Unit) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatchers.default) { load() }
    }

    private fun start(
        screen: PieChartStatisticScreen
    ) {
        loadLatest {
            budgetPage = screen.budgetPage
            // The split last chosen, by category or by account.
            breakdown = if (appPreferences.pieChartByAccount()) Breakdown.Accounts else Breakdown.Categories
            // Read each time the page shows, so one edited or deleted from it shows as it is now.
            val budgetExpenses = screen.budgetPage?.let { page ->
                page.budget.expensesIn(transactionRepository.findAllBetween(page.range.from, page.range.to))
            }
            startInternally(
                period = periodState.selected,
                type = screen.type,
                // The budget's own accounts: the chart then counts all its expenses, as the budget does.
                accountIdFilterList = budgetExpenses?.map { it.account.value }?.distinct()?.toImmutableList()
                    ?: screen.accountList,
                transactions = budgetExpenses?.toImmutableList() ?: screen.transactions,
                transfersAsIncomeExpenseValue = screen.treatTransfersAsIncomeExpense
            )
        }
    }

    private suspend fun startInternally(
        period: TimePeriod,
        type: TransactionType,
        accountIdFilterList: ImmutableList<UUID>,
        transactions: ImmutableList<Transaction>,
        transfersAsIncomeExpenseValue: Boolean
    ) {
        initialise(period, type, accountIdFilterList, transactions)
        treatTransfersAsIncomeExpense = transfersAsIncomeExpenseValue
        load(periodValue = period)
    }

    private suspend fun initialise(
        periodValue: TimePeriod,
        type: TransactionType,
        accountIdFilterListValue: ImmutableList<UUID>,
        transactionsValue: ImmutableList<Transaction>
    ) {
        val baseCurrencyValue = currencyRepository.getBaseCurrency().code

        period = periodValue
        transactionType = type
        accountIdFilterList = accountIdFilterListValue
        transactions = transactionsValue
        showCloseButtonOnly = transactionsValue.isNotEmpty() || budgetPage != null
        baseCurrency = baseCurrencyValue
    }

    private suspend fun load(
        periodValue: TimePeriod
    ) {
        val type = transactionType
        val accountIdFilterList = accountIdFilterList
        val transactions = transactions
        val baseCurrency = baseCurrency
        val range = periodValue.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        val treatTransferAsIncExp = appPreferences.transfersAsIncomeExpense() &&
            accountIdFilterList.isNotEmpty() && treatTransfersAsIncomeExpense

        val pieChartData = ioThread {
            pieChartUseCase.pieChart(
                baseCurrency = baseCurrency,
                range = range,
                type = type,
                accountIdFilterList = accountIdFilterList,
                treatTransferAsIncExp = treatTransferAsIncExp,
                existingTransactions = transactions,
                onlyExisting = budgetPage != null,
            )
        }
        if (budgetPage != null) loadBudgetTransactions(baseCurrency, transactions)

        val totalAmountValue = pieChartData.totalAmount
        val categoryAmountsValue = pieChartData.categoryAmounts

        period = periodValue
        totalAmount = totalAmountValue
        categoryAmounts = categoryAmountsValue
        selectedCategory = null
        accountAmounts = pieChartData.accountAmounts
        showBreakdownToggle = pieChartData.accountsCovered > 1
        selectedAccount = null
    }

    // A budget's page lists its transactions under the chart, as Home lists its own.
    private suspend fun loadBudgetTransactions(baseCurrency: String, transactions: List<Transaction>) {
        baseData = AppBaseData(
            baseCurrency = baseCurrency,
            accounts = accountRepository.findAll().toImmutableList(),
            categories = categoryRepository.findAll().toImmutableList(),
        )
        history = transactionsWithDateDividersUseCase.withDateDividers(
            baseCurrency = baseCurrency,
            transactions = transactions
        ).toImmutableList()
    }

    private fun onSetPeriod(periodValue: TimePeriod) {
        periodState.select(periodValue)
        loadLatest { load(periodValue = periodValue) }
    }

    private fun nextMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        if (month != null) {
            val next = periodState.selectMonth(month, 1L, year)
            loadLatest { load(periodValue = next) }
        }
    }

    private fun previousMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        if (month != null) {
            val previous = periodState.selectMonth(month, -1L, year)
            loadLatest { load(periodValue = previous) }
        }
    }

    private suspend fun configureMonthModal(timePeriod: TimePeriod?) {
        val choosePeriodModalData = if (timePeriod != null) {
            ChoosePeriodModalData(period = timePeriod)
        } else {
            null
        }

        choosePeriodModal = choosePeriodModalData
    }

    // As an account's: a tap selects a category, Unspecified too; a second tap, or one off the slices,
    // clears it, and the selected category's card comes first.
    private fun onCategoryClicked(clicked: SelectedCategory?) {
        val selected = clicked.takeIf { it != selectedCategory }
        categoryAmounts = categoryAmounts
            .sortedByDescending { it.amount }
            .sortedByDescending { selected != null && it.category == selected.category }
            .toImmutableList()
        selectedCategory = selected
    }

    // The other split starts with nothing selected, and is the one the chart opens with next time.
    private suspend fun onBreakdownSelected(value: Breakdown) {
        breakdown = value
        selectedCategory = null
        selectedAccount = null
        appPreferences.setPieChartByAccount(value == Breakdown.Accounts)
    }

    // As a category: a tap selects an account, again or off the slices clears it, and the selected
    // account's card comes first.
    private fun onAccountClicked(clickedAccount: Account?) {
        val selected = clickedAccount.takeIf { it != selectedAccount }
        accountAmounts = accountAmounts
            .sortedByDescending { it.amount }
            .sortedByDescending { selected != null && it.account == selected }
            .toImmutableList()
        selectedAccount = selected
    }
}
