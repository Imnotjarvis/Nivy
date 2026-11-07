package com.nivy.transactions

import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.toArgb
import com.nivy.base.resource.ResourceProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.getFromAccount
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.CategoryRepository
import com.nivy.design.system.colors.RedLight
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.ui.R
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.utils.computationThread
import com.nivy.wallet.utils.ioThread
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import javax.inject.Inject

/** Loads a category's totals, history and due transactions in the period into the page's state. */
class CategoryPeriodLoader @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val periodState: PeriodState,
    private val categoryLogic: CategoryTransactionsLogic,
    private val transactionsWithDateDividersUseCase: TransactionsWithDateDividersUseCase,
    private val transactionsIncomeExpenseUseCase: TransactionsIncomeExpenseUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val resourceProvider: ResourceProvider,
) {
    internal suspend fun initForCategory(
        state: CategoryLoadState,
        categoryId: UUID,
        accountFilterList: List<UUID>,
    ) = with(state) {
        val accountFilterSet = accountFilterList.toSet()
        val initialCategory = ioThread {
            categoryRepository.findById(CategoryId(categoryId)) ?: error("category not found")
        }
        category.value = initialCategory
        val range = period.value.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        balance.doubleValue = ioThread {
            categoryLogic.calculateCategoryBalance(initialCategory, range, accountFilterSet)
        }

        income.doubleValue = ioThread {
            categoryLogic.calculateCategoryIncome(initialCategory, range, accountFilterSet)
        }

        expenses.doubleValue = ioThread {
            categoryLogic.calculateCategoryExpenses(initialCategory, range, accountFilterSet)
        }

        history.value = ioThread {
            categoryLogic.historyByCategoryAccountWithDateDividers(
                initialCategory,
                range,
                accountFilterSet = accountFilterList.toSet(),
            ).toImmutableList()
        }

        loadDue(initialCategory, range, accountFilterSet)
    }

    internal suspend fun initForCategoryWithTransactions(
        state: CategoryLoadState,
        categoryId: UUID,
        accountFilterList: List<UUID>,
        transactions: List<Transaction>,
    ) = with(state) {
        computationThread {
            initWithTransactions.value = true

            val trans = transactions.filter {
                it !is Transfer && it.category?.value == categoryId
            }

            val accountFilterSet = accountFilterList.toSet()
            val initialCategory = ioThread {
                categoryRepository.findById(CategoryId(categoryId)) ?: error("category not found")
            }
            category.value = initialCategory
            val range =
                period.value.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

            val incomeTrans = transactions.filter {
                it is Income && it.category?.value == categoryId
            }

            val expenseTrans = transactions.filter {
                it is Expense && it.category?.value == categoryId
            }

            balance.doubleValue = ioThread {
                categoryLogic.calculateCategoryBalance(
                    initialCategory,
                    range,
                    accountFilterSet,
                    transactions = trans
                )
            }

            income.doubleValue = ioThread {
                categoryLogic.calculateCategoryIncome(
                    incomeTransaction = incomeTrans,
                    accountFilterSet = accountFilterSet
                )
            }

            expenses.doubleValue = ioThread {
                categoryLogic.calculateCategoryExpenses(
                    expenseTransactions = expenseTrans,
                    accountFilterSet = accountFilterSet
                )
            }

            history.value = ioThread {
                categoryLogic.historyByCategoryAccountWithDateDividers(
                    initialCategory,
                    range,
                    accountFilterSet = accountFilterList.toSet(),
                    transactions = trans
                ).toImmutableList()
            }

            loadDue(initialCategory, range, accountFilterSet)
        }
    }

    internal suspend fun initForUnspecifiedCategory(state: CategoryLoadState) = with(state) {
        val range = period.value.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        balance.doubleValue = ioThread {
            categoryLogic.calculateUnspecifiedBalance(range)
        }

        income.doubleValue = ioThread {
            categoryLogic.calculateUnspecifiedIncome(range)
        }

        expenses.doubleValue = ioThread {
            categoryLogic.calculateUnspecifiedExpenses(range)
        }

        history.value = ioThread {
            categoryLogic.historyUnspecified(range).toImmutableList()
        }

        // Upcoming
        upcomingIncome.doubleValue = ioThread {
            categoryLogic.calculateUpcomingIncomeUnspecified(range)
        }

        upcomingExpenses.doubleValue = ioThread {
            categoryLogic.calculateUpcomingExpensesUnspecified(range)
        }

        upcoming.value = ioThread {
            categoryLogic.upcomingUnspecified(range).toImmutableList()
        }

        // Overdue
        overdueIncome.doubleValue = ioThread {
            categoryLogic.calculateOverdueIncomeUnspecified(range)
        }

        overdueExpenses.doubleValue = ioThread {
            categoryLogic.calculateOverdueExpensesUnspecified(range)
        }

        overdue.value = ioThread { categoryLogic.overdueUnspecified(range).toImmutableList() }
    }

    /** A pie chart's "Unspecified" slice: its transactions without a category, in its accounts. */
    internal suspend fun initForUnspecifiedWithTransactions(
        state: CategoryLoadState,
        accountFilterList: List<UUID>,
        transactions: List<Transaction>,
    ) = with(state) {
        initWithTransactions.value = true
        val accountFilterSet = accountFilterList.toSet()
        val trans = transactions.filter { it !is Transfer && it.category == null }

        income.doubleValue = categoryLogic.calculateCategoryIncome(
            incomeTransaction = trans.filterIsInstance<Income>(),
            accountFilterSet = accountFilterSet,
        )
        expenses.doubleValue = categoryLogic.calculateCategoryExpenses(
            expenseTransactions = trans.filterIsInstance<Expense>(),
            accountFilterSet = accountFilterSet,
        )
        balance.doubleValue = income.doubleValue - expenses.doubleValue
        history.value = transactionsWithDateDividersUseCase.withDateDividers(
            baseCurrency = baseCurrency.value,
            transactions = trans.filter {
                it.settled && (accountFilterSet.isEmpty() || it.getFromAccount().value in accountFilterSet)
            }
        ).toImmutableList()
    }

    internal suspend fun initForAccountTransfersCategory(
        state: CategoryLoadState,
        accountFilterList: List<UUID>,
        transactions: List<Transaction>,
    ) = with(state) {
        initWithTransactions.value = true
        val accountTransferCategory = Category(
            name = NotBlankTrimmedString.unsafe(resourceProvider.getString(R.string.account_transfers)),
            color = ColorInt(RedLight.toArgb()),
            icon = IconAsset.unsafe("transfer"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
        category.value = accountTransferCategory
        val accountFilterIdSet = accountFilterList.toHashSet()
        val trans = transactions.filter {
            it is Transfer && it.category == null && (
                    it.fromAccount.value in accountFilterIdSet || it.toAccount.value in accountFilterIdSet
                    )
        }

        val historyIncomeExpense = transactionsIncomeExpenseUseCase.incomeExpense(
            transactions = trans,
            accounts = accountFilterList.mapNotNull { accID -> accounts.value.find { it.id.value == accID } },
            baseCurrency = baseCurrency.value
        )

        income.doubleValue = historyIncomeExpense.transferIncome.toDouble()
        expenses.doubleValue = historyIncomeExpense.transferExpense.toDouble()
        balance.doubleValue = income.doubleValue - expenses.doubleValue
        history.value = transactionsWithDateDividersUseCase.withDateDividers(
            baseCurrency = baseCurrency.value,
            transactions = transactions.filter { it.settled }
        ).toImmutableList()
    }

    /**
     * The category's upcoming and overdue transactions in [range], of the [accounts] (all of them
     * when empty), and their totals.
     */
    private suspend fun CategoryLoadState.loadDue(category: Category, range: FromToTimeRange, accounts: Set<UUID>) {
        val upcomingList = ioThread { categoryLogic.upcomingByCategory(category, range, accounts) }
        upcomingIncome.doubleValue = categoryLogic.calculateCategoryIncome(upcomingList.filterIsInstance<Income>())
        upcomingExpenses.doubleValue = categoryLogic.calculateCategoryExpenses(upcomingList.filterIsInstance<Expense>())
        upcoming.value = upcomingList.toImmutableList()

        val overdueList = ioThread { categoryLogic.overdueByCategory(category, range, accounts) }
        overdueIncome.doubleValue = categoryLogic.calculateCategoryIncome(overdueList.filterIsInstance<Income>())
        overdueExpenses.doubleValue = categoryLogic.calculateCategoryExpenses(overdueList.filterIsInstance<Expense>())
        overdue.value = overdueList.toImmutableList()
    }
}

/** The view model's own state that loading a category fills in, for the loader to write. */
internal class CategoryLoadState(
    val period: State<TimePeriod>,
    val baseCurrency: State<String>,
    val accounts: State<ImmutableList<Account>>,
    val category: MutableState<Category?>,
    val initWithTransactions: MutableState<Boolean>,
    val balance: MutableDoubleState,
    val income: MutableDoubleState,
    val expenses: MutableDoubleState,
    val history: MutableState<ImmutableList<TransactionHistoryItem>>,
    val upcoming: MutableState<ImmutableList<Transaction>>,
    val upcomingIncome: MutableDoubleState,
    val upcomingExpenses: MutableDoubleState,
    val overdue: MutableState<ImmutableList<Transaction>>,
    val overdueIncome: MutableDoubleState,
    val overdueExpenses: MutableDoubleState,
)
