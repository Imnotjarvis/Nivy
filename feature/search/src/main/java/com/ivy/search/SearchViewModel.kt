package com.ivy.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.Account
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.ui.ComposeViewModel
import com.ivy.data.model.Category
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.ivy.wallet.utils.getDefaultFIATCurrency
import com.ivy.wallet.domain.data.TransactionHistoryItem
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
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionRepository: TransactionRepository,
    private val features: Features,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<SearchState, SearchEvent>() {

    private val transactions =
        mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
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

        return SearchState(
            searchQuery = searchQuery.value,
            transactions = transactions.value,
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
        }
    }

    private var searchJob: Job? = null

    private fun search(query: String) {
        val normalizedQuery = query.lowercase().trim()

        // Each keystroke searches again: stop the previous search, so that its results, which
        // may arrive later, can't replace the newer ones.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Nothing is listed until something is typed.
            val queryResult = if (normalizedQuery.isEmpty()) {
                persistentListOf()
            } else {
                withContext(dispatchers.io) {
                    val filteredTransactions = transactionRepository.findAll()
                        .filter { transaction ->
                            transaction.title.matchesQuery(normalizedQuery) ||
                                    transaction.description.matchesQuery(normalizedQuery)
                        }
                    transactionsWithDateDividersUseCase.withDateDividers(
                        baseCurrency = currencyRepository.getBaseCurrency().code,
                        transactions = filteredTransactions
                    ).toImmutableList()
                }
            }

            transactions.value = queryResult
            // Together with its results, so the screen tells what they were found for.
            searchQuery.value = query
            baseCurrency.value = currencyRepository.getBaseCurrency().code
            accounts.value = accountRepository.findAll().toImmutableList()
            categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    private fun NotBlankTrimmedString?.matchesQuery(query: String): Boolean {
        return this?.value?.lowercase()?.contains(query) == true
    }
}
