package com.nivy.budgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.budgets.model.DisplayBudget
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.expensesIn
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.navigation.BudgetScreen
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.toCloseTimeRange
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
class BudgetViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val budgetRepository: BudgetRepository,
    private val periodState: PeriodState,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionRepository: TransactionRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val userWrites: UserWrites,
) : ComposeViewModel<BudgetScreenState, BudgetScreenEvent>() {

    private val baseCurrency = mutableStateOf("")
    private val timeRange = mutableStateOf<FromToTimeRange?>(null)
    private val period = mutableStateOf<TimePeriod?>(null)
    private val budgets = mutableStateOf<ImmutableList<DisplayBudget>>(persistentListOf())
    private val categories = mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private val savings = mutableStateOf(SavingsGoal(goal = 0.0, saved = 0.0))
    private val reorderModalVisible = mutableStateOf(false)
    private val budgetModalData = mutableStateOf<BudgetModalData?>(null)

    // The latest load: a new one cancels it, so a slower load of another month can't land last.
    private var loadJob: Job? = null

    @Composable
    override fun uiState(): BudgetScreenState {
        return BudgetScreenState(
            baseCurrency = baseCurrency.value,
            categories = categories.value,
            accounts = accounts.value,
            budgets = budgets.value,
            savings = savings.value,
            timeRange = timeRange.value,
            period = period.value,
            reorderModalVisible = reorderModalVisible.value,
            budgetModalData = budgetModalData.value
        )
    }

    override fun onEvent(event: BudgetScreenEvent) {
        when (event) {
            is BudgetScreenEvent.OnOpen -> open(event.screen)

            is BudgetScreenEvent.OnCreateBudget -> {
                createBudget(event.name, event.amount, event.categories, event.accounts)
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

            is BudgetScreenEvent.OnPreviousMonth -> selectMonth(increment = -1)

            is BudgetScreenEvent.OnNextMonth -> selectMonth(increment = 1)

            is BudgetScreenEvent.OnSetSavingsGoal -> setSavingsGoal(event.amount)
        }
    }

    // The opening shown, which keeps its month: an opening of its own starts on the current month.
    private var opening: BudgetScreen? = null

    private fun open(screen: BudgetScreen) {
        if (screen != opening) period.value = null
        opening = screen
        start()
    }

    private fun start() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            categories.value = categoryRepository.findAll().toImmutableList()
            accounts.value = accountRepository.findAll().toImmutableList()
            val baseCurrency = currencyRepository.getBaseCurrency()
            val startDateOfMonth = appPreferences.startDayOfMonth().also(periodState::setStartDayOfMonth)
            // The month the user navigated to; the current one until they do.
            val selectedPeriod = period.value ?: TimePeriod.currentMonth(
                startDayOfMonth = startDateOfMonth,
                timeProvider = timeProvider,
            )
            val timeRange = selectedPeriod.toRange(startDateOfMonth = startDateOfMonth, timeConverter, timeProvider)
            val budgets = budgetRepository.findAll()

            val range = timeRange.toCloseTimeRange()
            val transactions = transactionRepository.findAllBetween(range.from, range.to)
            this@BudgetViewModel.budgets.value = budgets.map {
                val expenses = it.expensesIn(transactions)
                DisplayBudget(
                    budget = it,
                    spentAmount = expenses.sumOf { expense -> inBaseCurrency(expense.value, baseCurrency) },
                )
            }.toImmutableList()
            savings.value = SavingsGoal(
                goal = appPreferences.settings()?.bufferAmount ?: 0.0,
                saved = calculateSaved(transactions = transactions, baseCurrency = baseCurrency),
            )
            this@BudgetViewModel.baseCurrency.value = baseCurrency.code
            this@BudgetViewModel.timeRange.value = timeRange
            period.value = selectedPeriod
        }
    }

    private fun selectMonth(increment: Long) {
        val month = period.value?.month ?: return
        val year = period.value?.year ?: timeProvider.localDateNow().year
        period.value = month.incrementMonthPeriod(increment, year)
        start()
    }

    /**
     * What the month kept, as Home's income and expenses count it: income less expenses, paid ones
     * only, of the accounts in the balance. Moving money between accounts changes nothing.
     */
    private suspend fun calculateSaved(transactions: List<Transaction>, baseCurrency: AssetCode): Double {
        val counted = accounts.value.filter { it.includeInBalance }.map { it.id }.toSet()
        return transactions.filter { it.settled }.sumOf {
            when {
                it is Income && it.account in counted -> inBaseCurrency(it.value, baseCurrency)
                it is Expense && it.account in counted -> -inBaseCurrency(it.value, baseCurrency)
                else -> 0.0
            }
        }
    }

    private suspend fun inBaseCurrency(value: PositiveValue, baseCurrency: AssetCode): Double =
        exchangeUseCase.convert(value, to = baseCurrency).getOrNull()?.amount?.value ?: 0.0

    private fun setSavingsGoal(amount: Double) {
        viewModelScope.launch {
            appPreferences.setBufferAmount(amount)
            start()
        }
    }

    private fun createBudget(
        name: NotBlankTrimmedString,
        amount: PositiveDouble,
        categories: List<CategoryId>,
        accounts: List<AccountId>,
    ) = change {
        budgetRepository.save(
            Budget(
                id = BudgetId(UUID.randomUUID()),
                name = name,
                amount = amount,
                categories = categories,
                accounts = accounts,
                orderNum = budgetRepository.findMaxOrderNum() + 1,
            )
        )
    }

    private fun editBudget(budget: Budget) = change { budgetRepository.save(budget) }

    private fun deleteBudget(budget: Budget) = change { budgetRepository.deleteById(budget.id) }

    private fun reorder(newOrder: List<DisplayBudget>) = change {
        budgetRepository.saveMany(
            newOrder.mapIndexed { index, item ->
                item.budget.copy(orderNum = index.toDouble())
            }
        )
    }

    // A change the person made, then the budgets as they are now; a failure is shown instead.
    private fun change(write: suspend () -> Unit) {
        viewModelScope.launch {
            if (userWrites.run(R.string.msg_change_not_saved, write)) start()
        }
    }
}
