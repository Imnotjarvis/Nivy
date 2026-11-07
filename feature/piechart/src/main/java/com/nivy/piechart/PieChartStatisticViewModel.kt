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
import com.nivy.data.model.Category
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
    private var filterExcluded by mutableStateOf(false)
    private var transactions by mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private var choosePeriodModal by mutableStateOf<ChoosePeriodModalData?>(null)
    private var budgetPage by mutableStateOf<BudgetPage?>(null)
    private var history by mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
    private var baseData by mutableStateOf(AppBaseData("", persistentListOf(), persistentListOf()))

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
            filterExcluded = filterExcluded,
            transactions = transactions,
            choosePeriodModal = choosePeriodModal,
            budgetPage = budgetPage,
            history = history,
            baseData = baseData,
            compactTransactions = features.compactTransactionsMode.asEnabledState(),
            showAccountColors = features.showAccountColorsInTransactions.asEnabledState(),
        )
    }

    override fun onEvent(event: PieChartStatisticEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is PieChartStatisticEvent.OnSelectNextMonth -> nextMonth()
                is PieChartStatisticEvent.OnSelectPreviousMonth -> previousMonth()
                is PieChartStatisticEvent.OnSetPeriod -> onSetPeriod(event.timePeriod)
                is PieChartStatisticEvent.OnShowMonthModal -> configureMonthModal(event.timePeriod)
                is PieChartStatisticEvent.OnCategoryClicked -> onCategoryClicked(event.category)
                is PieChartStatisticEvent.OnStart -> start(event.screen)
            }
        }
    }

    private fun start(
        screen: PieChartStatisticScreen
    ) {
        viewModelScope.launch(dispatchers.default) {
            budgetPage = screen.budgetPage
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
                filterExclude = screen.filterExcluded,
                transactions = budgetExpenses?.toImmutableList() ?: screen.transactions,
                transfersAsIncomeExpenseValue = screen.treatTransfersAsIncomeExpense
            )
        }
    }

    private suspend fun startInternally(
        period: TimePeriod,
        type: TransactionType,
        accountIdFilterList: ImmutableList<UUID>,
        filterExclude: Boolean,
        transactions: ImmutableList<Transaction>,
        transfersAsIncomeExpenseValue: Boolean
    ) {
        initialise(period, type, accountIdFilterList, filterExclude, transactions)
        treatTransfersAsIncomeExpense = transfersAsIncomeExpenseValue
        load(periodValue = period)
    }

    private suspend fun initialise(
        periodValue: TimePeriod,
        type: TransactionType,
        accountIdFilterListValue: ImmutableList<UUID>,
        filterExcludedValue: Boolean,
        transactionsValue: ImmutableList<Transaction>
    ) {
        val baseCurrencyValue = currencyRepository.getBaseCurrency().code

        period = periodValue
        transactionType = type
        accountIdFilterList = accountIdFilterListValue
        filterExcluded = filterExcludedValue
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

    private suspend fun onSetPeriod(periodValue: TimePeriod) {
        periodState.select(periodValue)
        load(
            periodValue = periodValue
        )
    }

    private suspend fun nextMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        if (month != null) {
            load(
                periodValue = periodState.selectMonth(month, 1L, year)
            )
        }
    }

    private suspend fun previousMonth() {
        val month = period.month
        val year = period.year ?: timeProvider.localDateNow().year
        if (month != null) {
            load(
                periodValue = periodState.selectMonth(month, -1L, year)
            )
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

    private suspend fun onCategoryClicked(clickedCategory: Category?) {
        val selectedCategoryValue = if (clickedCategory == selectedCategory?.category) {
            null
        } else {
            clickedCategory?.let { SelectedCategory(category = it) }
        }

        val existingCategoryAmounts = categoryAmounts
        val newCategoryAmounts = if (selectedCategoryValue != null) {
            existingCategoryAmounts
                .sortedByDescending { it.amount }
                .sortedByDescending {
                    selectedCategoryValue.category == it.category
                }
        } else {
            existingCategoryAmounts.sortedByDescending {
                it.amount
            }
        }.toImmutableList()

        selectedCategory = selectedCategoryValue
        categoryAmounts = newCategoryAmounts
    }
}
