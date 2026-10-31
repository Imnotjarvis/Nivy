package com.ivy.budgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.budgets.model.DisplayBudget
import com.ivy.data.model.Budget
import com.ivy.data.model.BudgetId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Transaction
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.BudgetRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.wallet.PeriodState
import com.ivy.wallet.data.model.FromToTimeRange
import com.ivy.wallet.data.model.toCloseTimeRange
import com.ivy.wallet.utils.format
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.action.transaction.HistoryTrnsAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

@Stable
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val budgetRepository: BudgetRepository,
    private val periodState: PeriodState,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val historyTrnsAct: HistoryTrnsAct,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<BudgetScreenState, BudgetScreenEvent>() {

    private val baseCurrency = mutableStateOf("")
    private val timeRange = mutableStateOf<FromToTimeRange?>(null)
    private val budgets = mutableStateOf<ImmutableList<DisplayBudget>>(persistentListOf())
    private val categories = mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private val categoryBudgetsTotal = mutableDoubleStateOf(0.0)
    private val appBudgetMax = mutableDoubleStateOf(0.0)
    private val totalRemainingBudget = mutableDoubleStateOf(0.0)
    private val reorderModalVisible = mutableStateOf(false)
    private val budgetModalData = mutableStateOf<BudgetModalData?>(null)

    @Composable
    override fun uiState(): BudgetScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return BudgetScreenState(
            baseCurrency = getBaseCurrency(),
            categories = getCategories(),
            budgets = getBudgets(),
            categoryBudgetsTotal = getCategoryBudgetsTotal(),
            appBudgetMax = getAppBudgetMax(),
            totalRemainingBudgetText = getTotalRemainingBudgetText(),
            timeRange = getTimeRange(),
            reorderModalVisible = getReorderModalVisible(),
            budgetModalData = getBudgetModalData()
        )
    }

    @Composable
    private fun getBaseCurrency(): String {
        return baseCurrency.value
    }

    @Composable
    private fun getTimeRange(): FromToTimeRange? {
        return timeRange.value
    }

    @Composable
    private fun getCategories(): ImmutableList<Category> {
        return categories.value
    }

    @Composable
    private fun getBudgets(): ImmutableList<DisplayBudget> {
        return budgets.value
    }

    @Composable
    private fun getReorderModalVisible(): Boolean {
        return reorderModalVisible.value
    }

    @Composable
    private fun getCategoryBudgetsTotal(): Double {
        return categoryBudgetsTotal.doubleValue
    }

    @Composable
    private fun getAppBudgetMax(): Double {
        return appBudgetMax.doubleValue
    }

    @Composable
    private fun getTotalRemainingBudgetText(): String? {
        val budgetExceeded = totalRemainingBudget.doubleValue < 0
        return when {
            categoryBudgetsTotal.doubleValue > 0 -> stringResource(
                if (budgetExceeded) R.string.budget_exceeded_info else R.string.total_budget_info,
                abs(totalRemainingBudget.doubleValue).format(baseCurrency.value),
                baseCurrency.value
            )

            else -> null
        }
    }

    @Composable
    private fun getBudgetModalData(): BudgetModalData? {
        return budgetModalData.value
    }

    override fun onEvent(event: BudgetScreenEvent) {
        when (event) {
            is BudgetScreenEvent.OnCreateBudget -> {
                createBudget(event.name, event.amount, event.categories)
            }

            is BudgetScreenEvent.OnEditBudget -> {
                editBudget(event.budget)
            }

            is BudgetScreenEvent.OnDeleteBudget -> {
                deleteBudget(event.budget)
            }

            is BudgetScreenEvent.OnReorder -> {
                reorder(event.newOrder)
            }

            is BudgetScreenEvent.OnReorderModalVisible -> {
                reorderModalVisible.value = event.visible
            }

            is BudgetScreenEvent.OnBudgetModalData -> {
                budgetModalData.value = event.budgetModalData
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            categories.value = categoryRepository.findAll().toImmutableList()
            val baseCurrency = currencyRepository.getBaseCurrency()
            val startDateOfMonth = appPreferences.startDayOfMonth().also(periodState::setStartDayOfMonth)
            val timeRange = com.ivy.wallet.data.model.TimePeriod.currentMonth(
                startDayOfMonth = startDateOfMonth,
                timeProvider = timeProvider,
            ).toRange(startDateOfMonth = startDateOfMonth, timeConverter, timeProvider)
            val budgets = budgetRepository.findAll()

            appBudgetMax.doubleValue = budgets
                .filter { it.categories.isEmpty() }
                .maxOfOrNull { it.amount.value } ?: 0.0

            categoryBudgetsTotal.doubleValue = budgets
                .filter { it.categories.isNotEmpty() }
                .sumOf { it.amount.value }

            val transactions = historyTrnsAct(timeRange.toCloseTimeRange())
            this@BudgetViewModel.budgets.value = budgets.map {
                DisplayBudget(
                    budget = it,
                    spentAmount = calculateSpentAmount(
                        budget = it,
                        transactions = transactions,
                        baseCurrency = baseCurrency
                    )
                )
            }.toImmutableList()
            totalRemainingBudget.doubleValue = calculateTotalRemainingBudget(
                budgets = this@BudgetViewModel.budgets.value,
                categoryBudgetsTotal = categoryBudgetsTotal.doubleValue
            )
            this@BudgetViewModel.baseCurrency.value = baseCurrency.code
            this@BudgetViewModel.timeRange.value = timeRange
        }
    }

    /** Sums the budget's expenses in the base currency. Income and transfers don't count. */
    private suspend fun calculateSpentAmount(
        budget: Budget,
        transactions: List<Transaction>,
        baseCurrency: AssetCode,
    ): Double {
        return transactions
            .filterIsInstance<Expense>()
            .filter { budget.accounts.isEmpty() || it.getFromAccount() in budget.accounts }
            .filter { budget.categories.isEmpty() || it.category in budget.categories }
            .sumOf {
                exchangeUseCase.convert(it.value, to = baseCurrency).getOrNull()?.amount?.value ?: 0.0
            }
    }

    private fun createBudget(
        name: NotBlankTrimmedString,
        amount: PositiveDouble,
        categories: List<CategoryId>,
    ) {
        viewModelScope.launch {
            budgetRepository.save(
                Budget(
                    id = BudgetId(UUID.randomUUID()),
                    name = name,
                    amount = amount,
                    categories = categories,
                    accounts = emptyList(),
                    orderNum = budgetRepository.findMaxOrderNum() + 1,
                )
            )
            start()
        }
    }

    private fun editBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.save(budget)
            start()
        }
    }

    private fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteById(budget.id)
            start()
        }
    }

    private fun reorder(newOrder: List<DisplayBudget>) {
        viewModelScope.launch {
            budgetRepository.saveMany(
                newOrder.mapIndexed { index, item ->
                    item.budget.copy(orderNum = index.toDouble())
                }
            )
            start()
        }
    }
}

fun calculateTotalRemainingBudget(
    budgets: ImmutableList<DisplayBudget>,
    categoryBudgetsTotal: Double
): Double {
    return categoryBudgetsTotal - budgets
        .filter { it.budget.categories.isNotEmpty() }
        .sumOf { it.spentAmount }
}
