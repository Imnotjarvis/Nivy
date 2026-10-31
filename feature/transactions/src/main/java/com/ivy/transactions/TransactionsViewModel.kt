package com.ivy.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import arrow.core.toOption
import com.ivy.base.resource.ResourceProvider
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.PlannedPaymentRuleRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.design.system.colors.RedLight
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.planned.DueTransactionsUseCase
import com.ivy.wallet.PeriodState
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.data.model.toCloseTimeRange
import com.ivy.wallet.domain.logic.AccountCreator
import com.ivy.wallet.utils.computationThread
import com.ivy.wallet.utils.ioThread
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.navigation.Navigation
import com.ivy.navigation.TransactionsScreen
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.action.account.AccTrnsAct
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.account.CalcAccIncomeExpenseAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.CalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.logic.CategoryCreator
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// The transactions screen serves accounts and categories with their own stats and actions.
@Suppress("LargeClass")
@Stable
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val periodState: PeriodState,
    private val nav: Navigation,
    private val accountLogic: AccountTransactionsLogic,
    private val categoryLogic: CategoryTransactionsLogic,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val appPreferences: AppPreferences,
    private val accountsAct: AccountsAct,
    private val accTrnsAct: AccTrnsAct,
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val calcAccIncomeExpenseAct: CalcAccIncomeExpenseAct,
    private val calcTrnsIncomeExpenseAct: CalcTrnsIncomeExpenseAct,
    private val exchangeAct: ExchangeAct,
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val features: Features,
    private val resourceProvider: ResourceProvider,
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
    private val category = mutableStateOf<Category?>(null)
    private val initWithTransactions = mutableStateOf(false)
    private val treatTransfersAsIncomeExpense = mutableStateOf(false)
    private val accountNameConfirmation = mutableStateOf(selectEndTextFieldValue(""))
    private val enableDeletionButton = mutableStateOf(false)
    private val skipAllModalVisible = mutableStateOf(false)
    private val deleteModal1Visible = mutableStateOf(false)
    private val choosePeriodModal = mutableStateOf<ChoosePeriodModalData?>(null)

    @Composable
    override fun uiState(): TransactionsState {
        return TransactionsState(
            period = getPeriod(),
            baseCurrency = getBaseCurrency(),
            currency = getCurrency(),
            categories = getCategories(),
            accounts = getAccounts(),
            account = getAccount(),
            category = getCategory(),
            balance = getBalance(),
            balanceBaseCurrency = getBalanceBaseCurrency(),
            income = getIncome(),
            expenses = getExpenses(),
            initWithTransactions = getInitWithTransactions(),
            treatTransfersAsIncomeExpense = getTreatTransfersAsIncomeExpense(),
            history = getHistory(),
            upcoming = getUpcoming(),
            upcomingExpanded = getUpcomingExpanded(),
            upcomingIncome = getUpcomingIncome(),
            upcomingExpenses = getUpcomingExpenses(),
            overdue = getOverdue(),
            overdueExpanded = getOverdueExpanded(),
            overdueIncome = getOverdueIncome(),
            overdueExpenses = getOverdueExpenses(),
            enableDeletionButton = getEnableDeletionButton(),
            skipAllModalVisible = getSkipAllModalVisible(),
            deleteModal1Visible = getDeleteModal1Visible(),
            choosePeriodModal = getChoosePeriodModal(),
            showAccountColorsInTransactions = getShouldShowAccountSpecificColorInTransactions()
        )
    }

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
    }
    private fun getPeriod(): TimePeriod {
        return period.value
    }
    private fun getBaseCurrency(): String {
        return baseCurrency.value
    }
    private fun getAccount(): Account? {
        return account.value
    }
    private fun getCurrency(): String {
        return currency.value
    }
    private fun getCategories(): ImmutableList<Category> {
        return categories.value
    }
    private fun getAccounts(): ImmutableList<Account> {
        return accounts.value
    }
    private fun getCategory(): Category? {
        return category.value
    }
    private fun getBalance(): Double {
        return balance.doubleValue
    }
    private fun getBalanceBaseCurrency(): Double? {
        return balanceBaseCurrency.value
    }
    private fun getIncome(): Double {
        return income.doubleValue
    }
    private fun getExpenses(): Double {
        return expenses.doubleValue
    }
    private fun getInitWithTransactions(): Boolean {
        return initWithTransactions.value
    }
    private fun getTreatTransfersAsIncomeExpense(): Boolean {
        return treatTransfersAsIncomeExpense.value
    }
    private fun getUpcomingExpenses(): Double {
        return upcomingExpenses.doubleValue
    }
    private fun getUpcoming(): ImmutableList<Transaction> {
        return upcoming.value
    }
    private fun getUpcomingExpanded(): Boolean {
        return upcomingExpanded.value
    }
    private fun getUpcomingIncome(): Double {
        return upcomingIncome.doubleValue
    }
    private fun getHistory(): ImmutableList<TransactionHistoryItem> {
        return history.value
    }
    private fun getOverdue(): ImmutableList<Transaction> {
        return overdue.value
    }
    private fun getOverdueExpanded(): Boolean {
        return overdueExpanded.value
    }
    private fun getOverdueIncome(): Double {
        return overdueIncome.doubleValue
    }
    private fun getOverdueExpenses(): Double {
        return overdueExpenses.doubleValue
    }
    private fun getEnableDeletionButton(): Boolean {
        return enableDeletionButton.value
    }
    private fun getSkipAllModalVisible(): Boolean {
        return skipAllModalVisible.value
    }
    private fun getDeleteModal1Visible(): Boolean {
        return deleteModal1Visible.value
    }
    private fun getChoosePeriodModal(): ChoosePeriodModalData? {
        return choosePeriodModal.value
    }

    override fun onEvent(event: TransactionsEvent) {
        when (event) {
            is TransactionsEvent.Delete -> delete(event.screen)
            is TransactionsEvent.EditAccount -> editAccount(
                event.screen,
                event.account,
                event.newBalance
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
            is TransactionsEvent.SetSkipAllModalVisible -> setSkipAllModalVisible(event.visible)
            is TransactionsEvent.OnDeleteModal1Visible -> setDeleteModal1Visible(event.delete)
            is TransactionsEvent.OnChoosePeriodModalData -> setChoosePeriodModalData(event.data)
        }
    }

    private suspend fun initForAccount(accountId: UUID) {
        val initialAccount = accountRepository.findById(AccountId(accountId))
            ?: error("account not found")
        account.value = initialAccount
        val range = period.value.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        currency.value = initialAccount.asset.code

        val balanceValue = calcAccBalanceAct(
            CalcAccBalanceAct.Input(
                account = initialAccount
            )
        ).balance.toDouble()
        balance.doubleValue = balanceValue
        if (baseCurrency.value != currency.value) {
            balanceBaseCurrency.value = exchangeAct(
                ExchangeAct.Input(
                    data = ExchangeData(
                        baseCurrency = baseCurrency.value,
                        fromCurrency = currency.value.toOption()
                    ),
                    amount = balanceValue.toBigDecimal()
                )
            ).getOrNull()?.toDouble()
        }

        val includeTransfersInCalc = appPreferences.transfersAsIncomeExpense()

        val incomeExpensePair = calcAccIncomeExpenseAct(
            CalcAccIncomeExpenseAct.Input(
                account = initialAccount,
                range = range.toCloseTimeRange(),
                includeTransfersInCalc = includeTransfersInCalc
            )
        ).incomeExpensePair
        income.doubleValue = incomeExpensePair.income.toDouble()
        expenses.doubleValue = incomeExpensePair.expense.toDouble()

        val accountTransactions = accTrnsAct(
            AccTrnsAct.Input(
                accountId = initialAccount.id.value,
                range = range.toCloseTimeRange()
            )
        )
        history.value = trnsWithDateDivsAct(
            TrnsWithDateDivsAct.Input(
                baseCurrency = baseCurrency.value,
                transactions = accountTransactions.filter { it.settled }
            )
        ).toImmutableList()

        // Upcoming
        upcomingIncome.doubleValue = ioThread {
            accountLogic.calculateUpcomingIncome(initialAccount, range)
        }

        upcomingExpenses.doubleValue = ioThread {
            accountLogic.calculateUpcomingExpenses(initialAccount, range)
        }

        upcoming.value = ioThread {
            accountLogic.upcoming(initialAccount, range).toImmutableList()
        }

        // Overdue
        overdueIncome.doubleValue = ioThread {
            accountLogic.calculateOverdueIncome(initialAccount, range)
        }

        overdueExpenses.doubleValue = ioThread {
            accountLogic.calculateOverdueExpenses(initialAccount, range)
        }

        overdue.value = ioThread {
            accountLogic.overdue(initialAccount, range).toImmutableList()
        }
    }

    private suspend fun initForCategory(categoryId: UUID, accountFilterList: List<UUID>) {
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

        // Upcoming
        // TODO: Rework Upcoming to FP
        upcomingIncome.doubleValue = ioThread {
            categoryLogic.calculateUpcomingIncomeByCategory(initialCategory, range)
        }

        upcomingExpenses.doubleValue = ioThread {
            categoryLogic.calculateUpcomingExpensesByCategory(initialCategory, range)
        }

        upcoming.value = ioThread {
            categoryLogic.upcomingByCategory(initialCategory, range).toImmutableList()
        }

        // Overdue
        // TODO: Rework Overdue to FP
        overdueIncome.doubleValue = ioThread {
            categoryLogic.calculateOverdueIncomeByCategory(initialCategory, range)
        }

        overdueExpenses.doubleValue = ioThread {
            categoryLogic.calculateOverdueExpensesByCategory(initialCategory, range)
        }

        overdue.value =
            ioThread {
                categoryLogic.overdueByCategory(initialCategory, range).toImmutableList()
            }
    }

    private suspend fun initForCategoryWithTransactions(
        categoryId: UUID,
        accountFilterList: List<UUID>,
        transactions: List<Transaction>,
    ) {
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

            balance.value = ioThread {
                categoryLogic.calculateCategoryBalance(
                    initialCategory,
                    range,
                    accountFilterSet,
                    transactions = trans
                )
            }

            income.value = ioThread {
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

            // Upcoming
            // TODO: Rework Upcoming to FP
            upcomingIncome.doubleValue = ioThread {
                categoryLogic.calculateUpcomingIncomeByCategory(initialCategory, range)
            }

            upcomingExpenses.doubleValue = ioThread {
                categoryLogic.calculateUpcomingExpensesByCategory(initialCategory, range)
            }

            upcoming.value = ioThread {
                categoryLogic.upcomingByCategory(initialCategory, range).toImmutableList()
            }

            // Overdue
            // TODO: Rework Overdue to FP
            overdueIncome.doubleValue = ioThread {
                categoryLogic.calculateOverdueIncomeByCategory(initialCategory, range)
            }

            overdueExpenses.doubleValue = ioThread {
                categoryLogic.calculateOverdueExpensesByCategory(initialCategory, range)
            }

            overdue.value =
                ioThread {
                    categoryLogic.overdueByCategory(initialCategory, range).toImmutableList()
                }
        }
    }

    private suspend fun initForUnspecifiedCategory() {
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

        upcomingExpenses.value = ioThread {
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

    private suspend fun initForAccountTransfersCategory(
        accountFilterList: List<UUID>,
        transactions: List<Transaction>,
    ) {
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

        val historyIncomeExpense = calcTrnsIncomeExpenseAct(
            CalcTrnsIncomeExpenseAct.Input(
                transactions = trans,
                accounts = accountFilterList.mapNotNull { accID -> accounts.value.find { it.id.value == accID } },
                baseCurrency = baseCurrency.value
            )
        )

        income.doubleValue = historyIncomeExpense.transferIncome.toDouble()
        expenses.doubleValue = historyIncomeExpense.transferExpense.toDouble()
        balance.doubleValue = income.doubleValue - expenses.doubleValue
        history.value = trnsWithDateDivsAct(
            TrnsWithDateDivsAct.Input(
                baseCurrency = baseCurrency.value,
                transactions = transactions.filter { it.settled }
            )
        ).toImmutableList()
    }

    private fun reset() {
        account.value = null
        category.value = null
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
                    deleteAccount(screen.accountId!!)
                }

                screen.categoryId != null -> {
                    deleteCategory(screen.categoryId!!)
                }
            }
        }
    }

    private suspend fun deleteAccount(accountId: UUID) {
        ioThread {
            transactionRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            plannedPaymentRuleRepository.deleteAllByAccountId(accountId = AccountId(accountId))
            accountRepository.deleteById(AccountId(accountId))

            nav.back()
        }
    }

    private suspend fun deleteCategory(categoryId: UUID) {
        ioThread {
            categoryRepository.deleteById(CategoryId(categoryId))

            nav.back()
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
    ) {
        viewModelScope.launch {
            accountCreator.editAccount(account, newBalance) {
                start(
                    screen = screen,
                    timePeriod = period.value,
                    reset = false
                )
            }
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
        accountNameConfirmation.value = selectEndTextFieldValue(confirmationText)
        enableDeletionButton.value = account.value?.name?.value == confirmationText ||
                category.value?.name?.value == confirmationText
    }

    fun start(
        screen: TransactionsScreen,
        timePeriod: TimePeriod? = periodState.selected,
        reset: Boolean = true,
    ) {
        if (reset) {
            reset()
        }

        viewModelScope.launch {
            period.value = timePeriod ?: periodState.selected

            val baseCurrencyValue = baseCurrencyAct()
            baseCurrency.value = baseCurrencyValue
            currency.value = baseCurrency.value

            categories.value = categoryRepository.findAll().toImmutableList()
            accounts.value = accountsAct()
            initWithTransactions.value = false
            treatTransfersAsIncomeExpense.value = appPreferences.transfersAsIncomeExpense()

            when {
                screen.accountId != null -> {
                    initForAccount(screen.accountId!!)
                }

                screen.categoryId != null && screen.transactions.isEmpty() -> {
                    initForCategory(screen.categoryId!!, screen.accountIdFilterList)
                }
                // unspecifiedCategory==false is explicitly checked to accommodate for a temp
                // AccountTransfers Category during Reports Screen
                screen.categoryId != null && screen.transactions.isNotEmpty() &&
                        screen.unspecifiedCategory == false -> {
                    initForCategoryWithTransactions(
                        screen.categoryId!!,
                        screen.accountIdFilterList,
                        screen.transactions
                    )
                }

                screen.unspecifiedCategory == true && screen.transactions.isNotEmpty() -> {
                    initForAccountTransfersCategory(
                        screen.accountIdFilterList,
                        screen.transactions
                    )
                }

                screen.unspecifiedCategory == true -> {
                    initForUnspecifiedCategory()
                }

                else -> error("no id provided")
            }
        }
    }
}
