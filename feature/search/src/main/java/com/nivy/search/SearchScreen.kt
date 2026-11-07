package com.nivy.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.model.Theme
import com.nivy.wallet.NivyComponentPreview
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.SearchInput
import com.nivy.wallet.ui.component.transaction.transactions
import com.nivy.design.utils.keyboardHeight
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.navigation.NivyMaterial3Preview
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SearchScreen() {
    val viewModel: SearchViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    SearchUi(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun SearchUi(
    uiState: SearchState,
    focusSearch: Boolean = true,
    onEvent: (SearchEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))

        val listState = rememberLazyListState()

        var searchQueryTextFieldValue by remember {
            mutableStateOf(selectEndTextFieldValue(uiState.searchQuery))
        }

        SearchInput(
            searchQueryTextFieldValue = searchQueryTextFieldValue,
            hint = stringResource(R.string.search_transactions),
            focus = focusSearch,
            showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
            onSetSearchQueryTextField = {
                searchQueryTextFieldValue = it
                onEvent(SearchEvent.Search(it.text))
            }
        )

        LaunchedEffect(uiState.transactions) {
            // scroll to top when transactions are changed
            listState.animateScrollToItem(index = 0, scrollOffset = 0)
        }

        Spacer(Modifier.height(16.dp))
        val emptyStateTitle = stringResource(R.string.no_transactions)
        val emptyStateText = stringResource(
            R.string.no_transactions_for_query,
            uiState.searchQuery
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState

        ) {
            // Nothing until something is typed and searched for: then its results, or that none
            // were found.
            if (uiState.searchQuery.isNotBlank()) {
                transactions(
                    baseData = AppBaseData(
                        baseCurrency = uiState.baseCurrency,
                        accounts = uiState.accounts,
                        categories = uiState.categories
                    ),
                    upcoming = null,
                    setUpcomingExpanded = { },
                    overdue = null,
                    setOverdueExpanded = { },
                    history = uiState.transactions,
                    onPayOrGet = { },
                    emptyStateTitle = emptyStateTitle,
                    emptyStateText = emptyStateText,
                    dateDividerMarginTop = 16.dp,
                    shouldShowAccountSpecificColorInTransactions = uiState.shouldShowAccountSpecificColorInTransactions,
                    compact = uiState.compactTransactions,
                )
            }

            item {
                // Follows the keyboard as it opens and closes.
                val keyboardShownInsetDp = keyboardHeight()

                Spacer(Modifier.height(keyboardShownInsetDp))
                // add keyboard height margin at bototm so the list can scroll to bottom
            }
        }
    }
}

@Preview
@Composable
private fun Preview(isDark: Boolean = false) {
    NivyMaterial3Preview(isDark) {
        SearchUi(
            uiState = SearchState(
                searchQuery = "",
                transactions = persistentListOf(),
                baseCurrency = "",
                accounts = persistentListOf(),
                categories = persistentListOf(),
                shouldShowAccountSpecificColorInTransactions = false
            ),
            // Focusing asks for the keyboard, and the screenshot tests' renderer can crash
            // starting it (the input method's latency tracker, on a thread it starts).
            focusSearch = false,
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    NivyComponentPreview(theme = theme) {
        Preview(isDark)
    }
}
