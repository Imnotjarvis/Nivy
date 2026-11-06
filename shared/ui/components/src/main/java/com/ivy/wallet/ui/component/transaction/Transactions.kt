package com.ivy.wallet.ui.component.transaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Transaction
import com.ivy.data.temp.migration.getTransactionType
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.data.DueSection
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.data.TransactionHistoryEntry
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.design.system.colors.Black
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.colors.Palette
import com.ivy.design.system.motion.appearTween

// With this many transactions or fewer, the list gets a taller spacer at the end so it can scroll.
private const val FewTransactions = 5

fun LazyListScope.transactions(
    baseData: AppBaseData,

    upcoming: DueSection?,
    overdue: DueSection?,
    history: List<TransactionHistoryItem>,

    emptyStateTitle: String,
    emptyStateText: String,

    dateDividerMarginTop: Dp? = null,
    lastItemSpacer: Dp? = null,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    setUpcomingExpanded: (Boolean) -> Unit,
    setOverdueExpanded: (Boolean) -> Unit,
    onSkipTransaction: (Transaction) -> Unit = {},
    onSkipAllTransactions: (List<Transaction>) -> Unit = {},
    // The history as compact rows; upcoming and overdue keep their cards, with Pay and Skip.
    compact: Boolean = false,
    // Items fade in as they're added: for a list that fills in after its screen shows.
    fadeIn: Boolean = false,
) {
    upcomingSection(
        baseData = baseData,
        upcoming = upcoming,
        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
        onPayOrGet = onPayOrGet,
        onSkipTransaction = onSkipTransaction,
        setExpanded = setUpcomingExpanded,
        fadeIn = fadeIn,
    )

    overdueSection(
        baseData = baseData,
        overdue = overdue,

        onPayOrGet = onPayOrGet,
        onSkipTransaction = onSkipTransaction,
        onSkipAllTransactions = onSkipAllTransactions,
        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
        setExpanded = setOverdueExpanded,
        fadeIn = fadeIn,
    )

    historySection(
        baseData = baseData,

        history = history,
        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
        dateDividerMarginTop = dateDividerMarginTop,
        onPayOrGet = onPayOrGet,
        compact = compact,
        fadeIn = fadeIn,
    )

    if (upcoming?.trns.isNullOrEmpty() && overdue?.trns.isNullOrEmpty() && history.isEmpty()) {
        item {
            FadingIn(fadeIn) {
                NoTransactionsEmptyState(
                    emptyStateTitle = emptyStateTitle,
                    emptyStateText = emptyStateText
                )
            }
        }
    }

    scrollHackSpacer(
        history = history,
        upcoming = upcoming,
        overdue = overdue,
        lastItemSpacer = lastItemSpacer
    )
}

private fun LazyListScope.upcomingSection(
    baseData: AppBaseData,

    upcoming: DueSection?,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    setExpanded: (Boolean) -> Unit,
    fadeIn: Boolean,
) {
    if (upcoming == null) return // guard

    if (upcoming.trns.isNotEmpty()) {
        item(
            key = "upcoming_section"
        ) {
            FadingIn(fadeIn) {
                SectionDivider(
                    expanded = upcoming.expanded,
                    setExpanded = setExpanded,
                    title = stringResource(R.string.upcoming),
                    titleColor = Orange,
                    baseCurrency = baseData.baseCurrency,
                    income = upcoming.stats.income.toDouble(),
                    expenses = upcoming.stats.expense.abs().toDouble()
                )
            }
        }

        if (upcoming.expanded) {
            trnItems(
                baseData = baseData,

                transactions = upcoming.trns,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction,
                fadeIn = fadeIn,
            )
        }
    }
}

private fun LazyListScope.overdueSection(
    baseData: AppBaseData,

    overdue: DueSection?,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    setExpanded: (Boolean) -> Unit,
    fadeIn: Boolean,
) {
    if (overdue == null) return

    if (overdue.trns.isNotEmpty()) {
        item(
            key = "overdue_section"
        ) {
            FadingIn(fadeIn) {
                SectionDivider(
                    expanded = overdue.expanded,
                    setExpanded = setExpanded,
                    title = stringResource(R.string.overdue),
                    titleColor = Palette.Red,
                    baseCurrency = baseData.baseCurrency,
                    income = overdue.stats.income.toDouble(),
                    expenses = overdue.stats.expense.abs().toDouble()
                )
            }
        }

        if (overdue.expanded) {
            item {
                FadingIn(fadeIn) {
                    val isLightTheme = MaterialTheme.colorScheme.surface == White
                    IvyButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.skip_all),
                        wrapContentMode = false,
                        backgroundColor = if (isLightTheme) White else Black,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isLightTheme) Black else White,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        onSkipAllTransactions(overdue.trns)
                    }
                }
            }

            trnItems(
                baseData = baseData,

                transactions = overdue.trns,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction,
                fadeIn = fadeIn,
            )
        }
    }
}

private fun LazyListScope.trnItems(
    baseData: AppBaseData,

    transactions: List<Transaction>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    fadeIn: Boolean,
) {
    items(
        items = transactions,
        key = { it.id.value }
    ) {
        val nav = navigation()
        FadingIn(fadeIn) {
            TransactionCard(
                baseData = baseData,

                transaction = it,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction
            ) { trn ->
                onTransactionClick(
                    nav = nav,
                    transaction = trn
                )
            }
        }
    }
}

private fun LazyListScope.historySection(
    baseData: AppBaseData,

    history: List<TransactionHistoryItem>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    dateDividerMarginTop: Dp? = null,

    onPayOrGet: (Transaction) -> Unit,
    compact: Boolean,
    fadeIn: Boolean,
) {
    if (history.isNotEmpty()) {
        items(
            items = history,
            key = {
                when (it) {
                    is TransactionHistoryEntry -> it.transaction.id.value.toString()
                    is TransactionHistoryDateDivider -> it.date.toString()
                }
            }
        ) {
            FadingIn(fadeIn) {
                when (it) {
                    is TransactionHistoryEntry -> {
                        val nav = navigation()

                        if (compact) {
                            CompactTransactionRow(
                                baseData = baseData,
                                transaction = it.transaction,
                                onClick = { trn -> onTransactionClick(nav = nav, transaction = trn) },
                            )
                        } else {
                            TransactionCard(
                                baseData = baseData,

                                transaction = it.transaction,
                                tags = it.tags,
                                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                                onPayOrGet = onPayOrGet
                            ) { trn ->
                                onTransactionClick(
                                    nav = nav,
                                    transaction = trn
                                )
                            }
                        }
                    }

                    is TransactionHistoryDateDivider -> {
                        HistoryDateDivider(
                            date = it.date,
                            spacerTop = dateDividerMarginTop
                                ?: if (it == history.firstOrNull()) 24.dp else 32.dp,
                            baseCurrency = baseData.baseCurrency,
                            income = it.income,
                            expenses = it.expenses
                        )
                    }
                }
            }
        }
    }
}

/**
 * [content], fading in as its item is added when [fadeIn]: rows that come in after the list shows
 * appear softly. Without placement or fade-out animations, other changes stay as they were.
 */
@Composable
private fun LazyItemScope.FadingIn(fadeIn: Boolean, content: @Composable () -> Unit) {
    if (fadeIn) {
        Box(Modifier.animateItem(fadeInSpec = appearTween(), placementSpec = null, fadeOutSpec = null)) {
            content()
        }
    } else {
        content()
    }
}

private fun onTransactionClick(
    nav: Navigation,
    transaction: Transaction
) {
    nav.navigateTo(
        EditTransactionScreen(
            initialTransactionId = transaction.id.value,
            type = transaction.getTransactionType()
        )
    )
}

@Composable
private fun LazyItemScope.NoTransactionsEmptyState(
    emptyStateTitle: String,
    emptyStateText: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        IvyIcon(
            icon = R.drawable.ic_notransactions,
            tint = Gray
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = emptyStateTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Gray,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = emptyStateText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(96.dp))
    }
}

private fun LazyListScope.scrollHackSpacer(
    history: List<TransactionHistoryItem>,
    upcoming: DueSection?,
    overdue: DueSection?,

    lastItemSpacer: Dp?,
) {
    item {
        if (lastItemSpacer != null) {
            Spacer(Modifier.height(lastItemSpacer))
        } else {
            // last spacer - scroll hack
            val trnCount = history.size.plus(
                if (upcoming != null && upcoming.expanded) upcoming.trns.size else 0
            ).plus(
                if (overdue != null && overdue.expanded) overdue.trns.size else 0
            )
            if (trnCount <= FewTransactions) {
                Spacer(Modifier.height(300.dp))
            } else {
                Spacer(Modifier.height(150.dp))
            }
        }
    }
}
