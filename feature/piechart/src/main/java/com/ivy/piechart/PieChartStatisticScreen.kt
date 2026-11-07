package com.ivy.piechart

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.ivy.wallet.ui.component.transaction.transactions
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.design.system.colors.Gray
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import androidx.compose.material3.MaterialTheme

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.PieChartStatisticScreen(
    screen: PieChartStatisticScreen
) {
    val viewModel: PieChartStatisticViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(PieChartStatisticEvent.OnStart(screen))
    }

    UI(
        state = uiState,
        lazyState = rememberScrollPositionListState(key = "item_pie_chart_lazy_column", screen = screen),
        onEvent = viewModel::onEvent
    )
}

@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.UI(
    state: PieChartStatisticState,
    lazyState: LazyListState = rememberLazyListState(),
    onEvent: (PieChartStatisticEvent) -> Unit = {}
) {
    val nav = navigation()
    // Recomposes when this flips, not on every scrolled item.
    val expanded by remember { derivedStateOf { lazyState.firstVisibleItemIndex < 1 } }
    val percentExpanded by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = com.ivy.wallet.utils.springBounce(),
        label = "percent expanded"
    )
    // The chart's order (the largest first), which gives each slice its tone in minimal colors.
    val bySize = remember(state.categoryAmounts) { state.categoryAmounts.sortedByDescending { it.amount } }
    val budgetPage = state.budgetPage
    val noTransactions = stringResource(R.string.no_transactions)
    val budgetNoExpenses = budgetPage?.let { stringResource(R.string.budget_no_expenses, it.month) }.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        state = lazyState
    ) {
        stickyHeader {
            Header(
                transactionType = state.transactionType,
                period = state.period,
                percentExpanded = percentExpanded,
                currency = state.baseCurrency,
                amount = state.totalAmount,
                onShowMonthModal = {
                    onEvent(PieChartStatisticEvent.OnShowMonthModal(state.period))
                },
                onSelectNextMonth = {
                    onEvent(PieChartStatisticEvent.OnSelectNextMonth)
                },
                onSelectPreviousMonth = {
                    onEvent(PieChartStatisticEvent.OnSelectPreviousMonth)
                },
                showCloseButtonOnly = state.showCloseButtonOnly,
                onClose = {
                    nav.back()
                },
                onAdd = { trnType ->
                    nav.navigateTo(
                        EditTransactionScreen(
                            initialTransactionId = null,
                            type = trnType
                        )
                    )
                }
            )
        }

        item {
            Spacer(Modifier.height(20.dp))

            Text(
                modifier = Modifier
                    .padding(start = 32.dp)
                    .testTag("piechart_title"),
                text = when {
                    budgetPage != null -> budgetPage.name
                    state.transactionType == TransactionType.EXPENSE -> stringResource(R.string.expenses)
                    else -> stringResource(R.string.income)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (budgetPage != null) {
                Text(
                    modifier = Modifier.padding(start = 32.dp),
                    text = budgetPage.month,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            BalanceRow(
                modifier = Modifier
                    .padding(start = 32.dp, end = 16.dp)
                    .testTag("piechart_total_amount")
                    .alpha(percentExpanded),
                currency = state.baseCurrency,
                balance = state.totalAmount,
                currencyUpfront = false
            )
        }

        item {
            Spacer(Modifier.height(40.dp))

            PieChart(
                type = state.transactionType,
                categoryAmounts = state.categoryAmounts,
                selectedCategory = state.selectedCategory,
                onCategoryClick = { clickedCategory ->
                    onEvent(PieChartStatisticEvent.OnCategoryClicked(clickedCategory))
                }
            )

            Spacer(Modifier.height(48.dp))
        }

        itemsIndexed(
            items = state.categoryAmounts,
            key = { _, item -> item.category?.id?.value ?: "unspecified" },
        ) { index, item ->
            if (item.amount != 0.0) {
                if (index != 0) {
                    Spacer(Modifier.height(16.dp))
                }

                CategoryAmountCard(
                    categoryAmount = item,
                    rank = bySize.indexOf(item),
                    currency = state.baseCurrency,
                    totalAmount = state.totalAmount,
                    selectedCategory = state.selectedCategory
                ) {
                    nav.navigateTo(
                        TransactionsScreen(
                            categoryId = item.category?.id?.value,
                            unspecifiedCategory = item.isCategoryUnspecified,
                            accountIdFilterList = state.accountIdFilterList,
                            transactions = item.associatedTransactions
                        )
                    )
                }
            }
        }

        if (budgetPage != null) {
            budgetTransactions(state = state, emptyTitle = noTransactions, emptyText = budgetNoExpenses)
        }

        item {
            Spacer(Modifier.height(160.dp)) // scroll hack
        }
    }

    ChoosePeriodModal(
        modal = state.choosePeriodModal,
        dismiss = {
            onEvent(PieChartStatisticEvent.OnShowMonthModal(null))
        }
    ) {
        onEvent(PieChartStatisticEvent.OnSetPeriod(it))
    }
}

// A budget's page lists its transactions under the chart, as Home does.
private fun LazyListScope.budgetTransactions(
    state: PieChartStatisticState,
    emptyTitle: String,
    emptyText: String,
) {
    item {
        Spacer(Modifier.height(32.dp))
    }

    transactions(
        baseData = state.baseData,
        upcoming = null,
        overdue = null,
        history = state.history,
        emptyStateTitle = emptyTitle,
        emptyStateText = emptyText,
        shouldShowAccountSpecificColorInTransactions = state.showAccountColors,
        onPayOrGet = {},
        setUpcomingExpanded = {},
        setOverdueExpanded = {},
        compact = state.compactTransactions,
    )
}
