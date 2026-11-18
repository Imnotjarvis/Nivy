package com.nivy.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.ui.ComposeViewModel
import com.nivy.data.model.Category
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.features.Features
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.ui.R
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.domain.pure.transaction.filterOverdue
import com.nivy.wallet.domain.pure.transaction.filterUpcoming
import com.nivy.wallet.utils.getDefaultFIATCurrency
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val transactionsWithDateDividersUseCase: TransactionsWithDateDividersUseCase,
    private val transactionsIncomeExpenseUseCase: TransactionsIncomeExpenseUseCase,
    private val dueTransactionsUseCase: DueTransactionsUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionRepository: TransactionRepository,
    private val userWrites: UserWrites,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<SearchState, SearchEvent>() {

    private val found = mutableStateOf(Found())
    private val upcomingExpanded = mutableStateOf(false)
    private val overdueExpanded = mutableStateOf(false)
    private val baseCurrency = mutableStateOf<String>(getDefaultFIATCurrency().currencyCode)
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private val categories = mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private val searchQuery = mutableStateOf("")

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
    }

    @Composable
    override fun uiState(): SearchState {
        LaunchedEffect(Unit) {
            search(searchQuery.value)
        }

        val found = found.value
        return SearchState(
            searchQuery = searchQuery.value,
            transactions = found.history,
            upcoming = DueSection(
                trns = found.upcoming,
                expanded = upcomingExpanded.value,
                stats = found.upcomingStats,
            ),
            overdue = DueSection(
                trns = found.overdue,
                expanded = overdueExpanded.value,
                stats = found.overdueStats,
            ),
            income = found.income,
            expenses = found.expenses,
            baseCurrency = baseCurrency.value,
            accounts = accounts.value,
            categories = categories.value,
            shouldShowAccountSpecificColorInTransactions = getShouldShowAccountSpecificColorInTransactions(),
            compactTransactions = features.compactTransactionsMode.asEnabledState(),
        )
    }

    override fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.Search -> search(event.query)
            is SearchEvent.SetUpcomingExpanded -> upcomingExpanded.value = event.expanded
            is SearchEvent.SetOverdueExpanded -> overdueExpanded.value = event.expanded
            is SearchEvent.PayOrGet -> write { dueTransactionsUseCase.pay(event.transaction) }
            is SearchEvent.Skip -> write { dueTransactionsUseCase.skip(event.transactions) }
        }
    }

    // A change to what was found, as the account's page makes it; then the search runs again.
    private fun write(change: suspend () -> Unit) {
        viewModelScope.launch {
            if (userWrites.run(R.string.msg_change_not_saved) { change() }) search(searchQuery.value)
        }
    }

    private var searchJob: Job? = null

    private fun search(query: String) {
        val normalizedQuery = query.lowercase().trim()

        // Each keystroke searches again: stop the previous search, so that its results, which
        // may arrive later, can't replace the newer ones.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val base = currencyRepository.getBaseCurrency().code
            val allAccounts = accountRepository.findAll()
            // Nothing is listed until something is typed.
            val queryResult = if (normalizedQuery.isEmpty()) {
                Found()
            } else {
                withContext(dispatchers.io) {
                    val filteredTransactions = transactionRepository.findAll()
                        .filter { transaction ->
                            transaction.title.matchesQuery(normalizedQuery) ||
                                    transaction.description.matchesQuery(normalizedQuery)
                        }
                    found(filteredTransactions, base, allAccounts)
                }
            }

            found.value = queryResult
            // Together with its results, so the screen tells what they were found for.
            searchQuery.value = query
            baseCurrency.value = base
            accounts.value = allAccounts.toImmutableList()
            categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    // What was paid, by day and with its totals, and apart, as an account's page lists them, the
    // planned payments still to pay: due from today on, the soonest first, and missed.
    private suspend fun found(matches: List<Transaction>, base: String, accounts: List<Account>): Found {
        suspend fun totals(transactions: List<Transaction>) =
            transactionsIncomeExpenseUseCase.incomeExpense(transactions, base, accounts)

        val paid = matches.filter { it.settled }
        val upcoming = matches.filterUpcoming(timeProvider).sortedBy { it.time }
        val overdue = matches.filterOverdue(timeProvider).sortedByDescending { it.time }
        val paidTotals = totals(paid)
        return Found(
            history = transactionsWithDateDividersUseCase.withDateDividers(base, paid).toImmutableList(),
            upcoming = upcoming.toImmutableList(),
            upcomingStats = totals(upcoming).let { IncomeExpensePair(it.income, it.expense) },
            overdue = overdue.toImmutableList(),
            overdueStats = totals(overdue).let { IncomeExpensePair(it.income, it.expense) },
            income = paidTotals.income.toDouble(),
            expenses = paidTotals.expense.toDouble(),
        )
    }

    private fun NotBlankTrimmedString?.matchesQuery(query: String): Boolean {
        return this?.value?.lowercase()?.contains(query) == true
    }
}

// What a search found: what was paid, by day and with its totals, and what's still to pay.
private data class Found(
    val history: ImmutableList<TransactionHistoryItem> = persistentListOf(),
    val upcoming: ImmutableList<Transaction> = persistentListOf(),
    val upcomingStats: IncomeExpensePair = IncomeExpensePair.zero(),
    val overdue: ImmutableList<Transaction> = persistentListOf(),
    val overdueStats: IncomeExpensePair = IncomeExpensePair.zero(),
    val income: Double = 0.0,
    val expenses: Double = 0.0,
)
