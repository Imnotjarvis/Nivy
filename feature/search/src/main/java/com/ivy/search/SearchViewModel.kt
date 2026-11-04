package com.ivy.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.data.model.Account
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.ui.ComposeViewModel
import com.ivy.data.model.Category
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.features.Features
import com.ivy.wallet.utils.getDefaultFIATCurrency
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import com.ivy.wallet.domain.data.TransactionHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val transactionRepository: TransactionRepository,
    private val features: Features
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
        searchQuery.value = query
        val normalizedQuery = query.lowercase().trim()

        // Each keystroke searches again: stop the previous search, so that its results, which
        // may arrive later, can't replace the newer ones.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val queryResult = ioThread {
                val filteredTransactions = transactionRepository.findAll()
                    .filter { transaction ->
                        transaction.title.matchesQuery(normalizedQuery) ||
                                transaction.description.matchesQuery(normalizedQuery)
                    }
                trnsWithDateDivsAct(
                    TrnsWithDateDivsAct.Input(
                        baseCurrency = baseCurrencyAct(),
                        transactions = filteredTransactions
                    )
                ).toImmutableList()
            }

            transactions.value = queryResult
            baseCurrency.value = baseCurrencyAct()
            accounts.value = accountsAct()
            categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    private fun NotBlankTrimmedString?.matchesQuery(query: String): Boolean {
        return this?.value?.lowercase()?.contains(query) == true
    }
}
