package com.nivy.reports

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import arrow.core.toOption
import com.nivy.base.Toaster
import com.nivy.base.model.TransactionType
import com.nivy.base.resource.ResourceProvider
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Tag
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.Transfer
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.temp.migration.getTransactionType
import com.nivy.data.temp.migration.getValue
import com.nivy.domain.RootScreen
import com.nivy.domain.features.Features
import com.nivy.domain.usecase.csv.ExportCsvUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.wallet.PeriodState
import com.nivy.wallet.domain.pure.transaction.filterOverdue
import com.nivy.wallet.domain.pure.transaction.filterUpcoming
import com.nivy.wallet.utils.getISOFormattedDateTime
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import com.nivy.wallet.domain.pure.util.orZero
import com.nivy.design.system.colors.Gray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val transactionRepository: TransactionRepository,
    private val periodState: PeriodState,
    private val exchangeUseCase: ExchangeUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionsWithDateDividersUseCase: TransactionsWithDateDividersUseCase,
    private val transactionsIncomeExpenseUseCase: TransactionsIncomeExpenseUseCase,
    private val currencyRepository: CurrencyRepository,
    private val tagRepository: TagRepository,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val features: Features,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatchersProvider,
    private val toaster: Toaster,
) : ComposeViewModel<ReportScreenState, ReportScreenEvent>() {
    private val unSpecifiedCategory =
        Category(
            name = NotBlankTrimmedString.unsafe(resourceProvider.getString(R.string.unspecified)),
            color = ColorInt(Gray.toArgb()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
    private var baseCurrency by mutableStateOf("")
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var historyIncomeExpense by mutableStateOf(IncomeExpenseTransferPair.zero())
    private var filter by mutableStateOf<ReportFilter?>(null)
    private var balance by mutableDoubleStateOf(0.0)
    private var income by mutableDoubleStateOf(0.0)
    private var expenses by mutableDoubleStateOf(0.0)
    private var upcomingIncome by mutableDoubleStateOf(0.0)
    private var upcomingExpenses by mutableDoubleStateOf(0.0)
    private var overdueIncome by mutableDoubleStateOf(0.0)
    private var overdueExpenses by mutableDoubleStateOf(0.0)
    private var history by mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
    private var upcomingTransactions by
    mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private var overdueTransactions by
    mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var upcomingExpanded by mutableStateOf(false)
    private var overdueExpanded by mutableStateOf(false)
    private var loading by mutableStateOf(false)
    private var accountIdFilters by mutableStateOf<ImmutableList<UUID>>(persistentListOf())
    private var transactions by mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private var filterOverlayVisible by mutableStateOf(false)
    private var showTransfersAsIncExpCheckbox by mutableStateOf(false)
    private var treatTransfersAsIncExp by mutableStateOf(false)
    private var allTags by mutableStateOf<ImmutableList<Tag>>(persistentListOf())

    private var tagSearchJob: Job? = null
    private val tagSearchDebounceTimeInMills: Long = 500

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
    }

    @Composable
    override fun uiState(): ReportScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return ReportScreenState(
            categories = categories,
            accounts = accounts,
            accountIdFilters = accountIdFilters,
            balance = balance,
            baseCurrency = baseCurrency,
            expenses = expenses,
            filter = filter,
            filterOverlayVisible = filterOverlayVisible,
            history = history,
            income = income,
            loading = loading,
            overdueExpanded = overdueExpanded,
            overdueExpenses = overdueExpenses,
            overdueIncome = overdueIncome,
            overdueTransactions = overdueTransactions,
            showTransfersAsIncExpCheckbox = showTransfersAsIncExpCheckbox,
            transactions = transactions,
            treatTransfersAsIncExp = treatTransfersAsIncExp,
            upcomingExpanded = upcomingExpanded,
            upcomingExpenses = upcomingExpenses,
            upcomingIncome = upcomingIncome,
            upcomingTransactions = upcomingTransactions,
            allTags = allTags,
            showAccountColorsInTransactions = getShouldShowAccountSpecificColorInTransactions(),
            compactTransactions = features.compactTransactionsMode.asEnabledState(),
        )
    }

    override fun onEvent(event: ReportScreenEvent) {
        viewModelScope.launch(dispatchers.default) {
            when (event) {
                is ReportScreenEvent.OnFilter -> setFilter(event.filter)
                is ReportScreenEvent.OnExport -> export(event.context)
                is ReportScreenEvent.OnPayOrGet -> payOrGet(event.transaction)
                is ReportScreenEvent.SkipTransaction -> skipTransactions(listOf(event.transaction))
                is ReportScreenEvent.SkipTransactions -> skipTransactions(event.transactions)
                is ReportScreenEvent.OnOverdueExpanded -> setOverdueExpandedValue(event.overdueExpanded)
                is ReportScreenEvent.OnUpcomingExpanded -> setUpcomingExpandedValue(event.upcomingExpanded)
                is ReportScreenEvent.OnFilterOverlayVisible -> setFilterOverlayVisibleValue(event.filterOverlayVisible)
                is ReportScreenEvent.OnTreatTransfersAsIncomeExpense -> onTreatTransfersAsIncomeExpense(
                    event.transfersAsIncomeExpense
                )

                is ReportScreenEvent.OnTagSearch -> onTagSearch(event.data)
            }
        }
    }

    // Each keystroke replaces the search still waiting: only what was typed last is searched, once
    // typing pauses.
    private fun onTagSearch(query: String) {
        tagSearchJob?.cancel()
        tagSearchJob = viewModelScope.launch(dispatchers.io) {
            delay(tagSearchDebounceTimeInMills)
            allTags = NotBlankTrimmedString.from(query.lowercase())
                .fold(
                    ifRight = { tagRepository.findByText(text = it.value) },
                    ifLeft = { tagRepository.findAll() },
                )
                .toImmutableList()
        }
    }

    private fun start() {
        viewModelScope.launch(dispatchers.io) {
            baseCurrency = currencyRepository.getBaseCurrency().code
            accounts = accountRepository.findAll().toImmutableList()
            categories =
                (listOf(unSpecifiedCategory) + categoryRepository.findAll()).toImmutableList()
            allTags = tagRepository.findAll().toImmutableList()
            // Data may have changed since the report was last shown.
            filter?.let { setFilter(it) }
        }
    }

    private suspend fun setFilter(reportFilter: ReportFilter?) {
        withContext(dispatchers.io) {
            if (reportFilter == null) {
                // The transfers checkbox recomputes the totals from it.
                historyIncomeExpense = IncomeExpenseTransferPair.zero()
                setReportValues(
                    income = 0.00,
                    expense = 0.00,
                    upcomingIncomeExpenseTransferPair = IncomeExpenseTransferPair.zero(),
                    overDueIncomeExpenseTransferPair = IncomeExpenseTransferPair.zero(),
                    history = persistentListOf(),
                    upcomingTransactions = persistentListOf(),
                    overdueTransactions = persistentListOf(),
                    accounts = accountRepository.findAll().toImmutableList(),
                    reportFilter = null,
                    accountIdFilters = persistentListOf(),
                    transactions = persistentListOf(),
                    balanceValue = 0.00
                )
                return@withContext
            }

            if (!reportFilter.validate()) return@withContext
            val tempAccounts = reportFilter.accounts
            val baseCurrency = baseCurrency
            loading = true

            val transactionsList = filterTransactions(
                baseCurrency = baseCurrency,
                filter = reportFilter
            )

            // History, totals and the pie charts count what was paid, as Home does. Unpaid planned
            // payments are listed under Upcoming and Overdue below.
            val settledTransactions = transactionsList.filter { it.settled }.toImmutableList()
            val tempHistory = settledTransactions
                .sortedByDescending { it.time }

            val historyWithDateDividers = async {
                transactionsWithDateDividersUseCase.withDateDividers(
                    baseCurrency = baseCurrency,
                    transactions = tempHistory
                )
            }

            historyIncomeExpense = transactionsIncomeExpenseUseCase.incomeExpense(
                transactions = tempHistory,
                accounts = tempAccounts,
                baseCurrency = baseCurrency
            )

            val tempIncome = historyIncomeExpense.income.toDouble() +
                    if (treatTransfersAsIncExp) historyIncomeExpense.transferIncome.toDouble() else 0.0

            val tempExpenses = historyIncomeExpense.expense.toDouble() +
                    if (treatTransfersAsIncExp) historyIncomeExpense.transferExpense.toDouble() else 0.0

            val tempBalance = calculateBalance(historyIncomeExpense).toDouble()

            val accountFilterIdList = async { reportFilter.accounts.map { it.id.value } }

            // Due today counts as upcoming, earlier days as overdue, as everywhere else.
            val upcomingTransactionsList = transactionsList.filterUpcoming(timeProvider)
                .sortedBy { it.time }
                .toImmutableList()

            val upcomingIncomeExpense = transactionsIncomeExpenseUseCase.incomeExpense(
                transactions = upcomingTransactionsList,
                accounts = tempAccounts,
                baseCurrency = baseCurrency
            )
            // Overdue
            val overdue = transactionsList.filterOverdue(timeProvider)
                .sortedByDescending { it.time }
                .toImmutableList()
            val overdueIncomeExpense = transactionsIncomeExpenseUseCase.incomeExpense(
                transactions = overdue,
                accounts = tempAccounts,
                baseCurrency = baseCurrency
            )

            setReportValues(
                income = tempIncome,
                expense = tempExpenses,
                upcomingIncomeExpenseTransferPair = upcomingIncomeExpense,
                overDueIncomeExpenseTransferPair = overdueIncomeExpense,
                history = historyWithDateDividers.await().toImmutableList(),
                upcomingTransactions = upcomingTransactionsList,
                overdueTransactions = overdue,
                // All of them: the filter lists every account to pick from.
                accounts = accountRepository.findAll().toImmutableList(),
                reportFilter = reportFilter,
                accountIdFilters = accountFilterIdList.await().toImmutableList(),
                transactions = settledTransactions,
                balanceValue = tempBalance
            )

            loading = false
        }
    }

    private fun setReportValues(
        income: Double,
        expense: Double,
        upcomingIncomeExpenseTransferPair: IncomeExpenseTransferPair,
        overDueIncomeExpenseTransferPair: IncomeExpenseTransferPair,
        history: ImmutableList<TransactionHistoryItem>,
        upcomingTransactions: ImmutableList<Transaction>,
        overdueTransactions: ImmutableList<Transaction>,
        accounts: ImmutableList<Account>,
        reportFilter: ReportFilter? = null,
        accountIdFilters: ImmutableList<UUID>,
        transactions: ImmutableList<Transaction>,
        balanceValue: Double
    ) {
        this.income = income
        this.expenses = expense
        this.upcomingExpenses = upcomingIncomeExpenseTransferPair.expense.toDouble()
        this.upcomingIncome = upcomingIncomeExpenseTransferPair.income.toDouble()
        this.overdueIncome = overDueIncomeExpenseTransferPair.income.toDouble()
        this.overdueExpenses = overDueIncomeExpenseTransferPair.expense.toDouble()
        this.history = history
        this.upcomingTransactions = upcomingTransactions
        this.overdueTransactions = overdueTransactions
        this.accounts = accounts
        this.filter = reportFilter
        this.accountIdFilters = accountIdFilters
        this.transactions = transactions
        this.balance = balanceValue
        this.showTransfersAsIncExpCheckbox =
            reportFilter?.trnTypes?.contains(TransactionType.TRANSFER) ?: false
    }

    // One pass that applies each of the report filter's criteria.
    @Suppress("CyclomaticComplexMethod")
    private suspend fun filterTransactions(
        baseCurrency: String,
        filter: ReportFilter,
    ): ImmutableList<Transaction> {
        val filterAccountIds = filter.accounts.map { it.id.value }
        val filterCategoryIds =
            filter.categories.map { if (it.id.value == unSpecifiedCategory.id.value) null else it.id }
        val filterRange =
            filter.period?.toRange(periodState.startDayOfMonth, timeConverter, timeProvider)

        val transactions = if (filter.includedTags.isNotEmpty()) {
            tagRepository.findByAllAssociatedIdForTagId(filter.includedTags)
                .asSequence()
                .flatMap { it.value }
                .map { TransactionId(it.associatedId.value) }
                .distinct()
                .toList()
                .let {
                    transactionRepository.findByIds(it)
                }
        } else {
            transactionRepository.findAll()
        }

        val excludeableByTagTransactionsIds = if (filter.excludedTags.isNotEmpty()) {
            tagRepository.findByAllAssociatedIdForTagId(filter.excludedTags)
                .asSequence()
                .flatMap { it.value }
                .distinct()
                .map { TransactionId(it.associatedId.value) }
                .toList()
                .let {
                    transactionRepository.findByIds(it)
                }.map {
                    it.id
                }
        } else {
            emptyList()
        }

        return transactions
            .filter { !excludeableByTagTransactionsIds.contains(it.id) }
            .filter { filter.trnTypes.contains(it.getTransactionType()) }
            .filter {
                // Filter by Time Period

                filterRange ?: return@filter false

                filterRange.includes(it.time)
            }
            .filter { trn ->
                // Filter by Accounts
                when (trn) {
                    is Transfer -> {
                        filterAccountIds.contains(trn.fromAccount.value) || // Transfers Out
                                (filterAccountIds.contains(trn.toAccount.value)) // Transfers In
                    }

                    is Expense -> {
                        filterAccountIds.contains(trn.account.value)
                    }

                    is Income -> {
                        filterAccountIds.contains(trn.account.value)
                    }
                }
            }
            .filter { trn ->
                // Filter by Categories

                filterCategoryIds.contains(trn.category) || trn is Transfer
            }
            .filter {
                // Filter by Amount
                // !NOTE: Amount must be converted to baseCurrency amount

                val trnAmountBaseCurrency = exchangeUseCase.exchange(
                    data = ExchangeData(
                        baseCurrency = baseCurrency,
                        fromCurrency = it.getFromValue().asset.code.toOption(),
                    ),
                    amount = it.getValue()
                ).orZero().toDouble()

                (filter.minAmount == null || trnAmountBaseCurrency >= filter.minAmount) &&
                        (filter.maxAmount == null || trnAmountBaseCurrency <= filter.maxAmount)
            }
            .filter {
                // Filter by Included Keywords

                val includeKeywords = filter.includeKeywords
                if (includeKeywords.isEmpty()) return@filter true

                it.title?.let { title ->
                    includeKeywords.forEach { keyword ->
                        if (title.value.containsLowercase(keyword)) {
                            return@filter true
                        }
                    }
                }

                it.description?.let { description ->
                    includeKeywords.forEach { keyword ->
                        if (description.value.containsLowercase(keyword)) {
                            return@filter true
                        }
                    }
                }

                false
            }
            .filter {
                // Filter by Excluded Keywords

                val excludedKeywords = filter.excludeKeywords
                if (excludedKeywords.isEmpty()) return@filter true

                it.title?.let { title ->
                    excludedKeywords.forEach { keyword ->
                        if (title.value.containsLowercase(keyword)) {
                            return@filter false
                        }
                    }
                }
                it.description?.let { description ->
                    excludedKeywords.forEach { keyword ->
                        if (description.value.containsLowercase(keyword)) {
                            return@filter false
                        }
                    }
                }
                true
            }.toImmutableList()
    }

    private fun String.containsLowercase(anotherString: String): Boolean {
        return this.lowercase().contains(anotherString.lowercase())
    }

    private fun calculateBalance(incomeExpenseTransferPair: IncomeExpenseTransferPair): BigDecimal {
        return with(incomeExpenseTransferPair) { income + transferIncome - expense - transferExpense }
    }

    private suspend fun export(context: Context) {
        val filter = filter ?: return
        if (!filter.validate()) return

        val rootScreen = context as RootScreen
        rootScreen.createNewFile(
            "NivyReport-${
                timeProvider.localNow().getISOFormattedDateTime()
            }.csv"
        ) { fileUri ->
            viewModelScope.launch {
                loading = true

                exportCsvUseCase.exportToFile(
                    outputFile = fileUri,
                    exportScope = {
                        filterTransactions(
                            baseCurrency = baseCurrency,
                            filter = filter
                        )
                    }
                ).fold(
                    ifLeft = { toaster.show(R.string.msg_export_failed) },
                    ifRight = { rootScreen.shareCSVFile(fileUri = fileUri) },
                )

                loading = false
            }
        }
    }

    private fun setUpcomingExpandedValue(expanded: Boolean) {
        upcomingExpanded = expanded
    }

    private fun setOverdueExpandedValue(expanded: Boolean) {
        overdueExpanded = expanded
    }

    private suspend fun payOrGet(transaction: Transaction) {
        dueTransactionsUseCase.pay(transaction)
        reloadReport()
    }

    private fun setFilterOverlayVisibleValue(visible: Boolean) {
        filterOverlayVisible = visible
    }

    private fun onTreatTransfersAsIncomeExpense(transfersAsIncExp: Boolean) {
        income = historyIncomeExpense.income.toDouble() +
                if (transfersAsIncExp) historyIncomeExpense.transferIncome.toDouble() else 0.0
        expenses = historyIncomeExpense.expense.toDouble() +
                if (transfersAsIncExp) historyIncomeExpense.transferExpense.toDouble() else 0.0
        treatTransfersAsIncExp = transfersAsIncExp
    }

    private suspend fun skipTransactions(transactions: List<Transaction>) {
        dueTransactionsUseCase.skip(transactions)
        reloadReport()
    }

    private suspend fun reloadReport() {
        withContext(dispatchers.main) {
            start()
            setFilter(filter)
        }
    }
}
