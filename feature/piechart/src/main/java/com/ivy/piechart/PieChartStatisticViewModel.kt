package com.ivy.piechart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Category
import com.ivy.data.model.Transaction
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.CurrencyRepository
import com.ivy.wallet.PeriodState
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.utils.ioThread
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.BudgetPage
import com.ivy.data.repository.CategoryRepository
import com.ivy.domain.features.Features
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.piechart.action.PieChartAct
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class PieChartStatisticViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val periodState: PeriodState,
    private val pieChartAct: PieChartAct,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct,
    private val features: Features,
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
        viewModelScope.launch(Dispatchers.Default) {
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
        viewModelScope.launch(Dispatchers.Default) {
            budgetPage = screen.budgetPage
            startInternally(
                period = periodState.selected,
                type = screen.type,
                accountIdFilterList = screen.accountList,
                filterExclude = screen.filterExcluded,
                transactions = screen.transactions,
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

        val pieChartActOutput = ioThread {
            pieChartAct(
                PieChartAct.Input(
                    baseCurrency = baseCurrency,
                    range = range,
                    type = type,
                    accountIdFilterList = accountIdFilterList,
                    treatTransferAsIncExp = treatTransferAsIncExp,
                    existingTransactions = transactions,
                    onlyExisting = budgetPage != null,
                )
            )
        }
        if (budgetPage != null) loadBudgetTransactions(baseCurrency, transactions)

        val totalAmountValue = pieChartActOutput.totalAmount
        val categoryAmountsValue = pieChartActOutput.categoryAmounts

        period = periodValue
        totalAmount = totalAmountValue
        categoryAmounts = categoryAmountsValue
        selectedCategory = null
    }

    // A budget's page lists its transactions under the chart, as Home lists its own.
    private suspend fun loadBudgetTransactions(baseCurrency: String, transactions: List<Transaction>) {
        baseData = AppBaseData(
            baseCurrency = baseCurrency,
            accounts = accountsAct().toImmutableList(),
            categories = categoryRepository.findAll().toImmutableList(),
        )
        history = trnsWithDateDivsAct(
            TrnsWithDateDivsAct.Input(baseCurrency = baseCurrency, transactions = transactions)
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
