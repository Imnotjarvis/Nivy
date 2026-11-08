package com.nivy.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CreditCard
import com.nivy.data.model.Transaction
import com.nivy.design.system.colors.isLight
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.thenIf
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.data.model.TimePeriod
import androidx.compose.foundation.lazy.items
import com.nivy.data.model.AccountValue
import com.nivy.design.system.colors.Nivy
import com.nivy.wallet.ui.component.transaction.SectionDivider
import com.nivy.wallet.ui.component.transaction.transactions
import com.nivy.wallet.utils.horizontalSwipeListener
import com.nivy.wallet.utils.rememberSwipeListenerState
import com.nivy.design.utils.setStatusBarDarkTextCompat
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.ui.rememberScrollPositionListState
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.design.system.colors.Gray
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.nivy.wallet.ui.theme.modal.edit.AccountModal
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.CategoryModal
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.nivy.design.system.colors.categoryColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.shapes.ShapeTop
import androidx.compose.runtime.DisposableEffect
import com.nivy.design.system.colors.accountColor

@Composable
fun BoxWithConstraintsScope.TransactionsScreen(screen: TransactionsScreen) {
    val viewModel: TransactionsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    val view = LocalView.current
    // Keyed on the screen: opening another account or category from here reuses this composable.
    LaunchedEffect(screen) {
        viewModel.start(screen)
    }
    // This screen colors the status bar after the account; put the theme's back when leaving.
    val lightTheme = MaterialTheme.colorScheme.isLight()
    DisposableEffect(Unit) {
        onDispose {
            setStatusBarDarkTextCompat(
                view = view,
                darkText = lightTheme
            )
        }
    }

    UI(
        screen = screen,
        period = uiState.period,
        baseCurrency = uiState.baseCurrency,
        currency = uiState.currency,

        categories = uiState.categories,
        accounts = uiState.accounts,

        account = uiState.account,
        creditCards = uiState.creditCards,
        category = uiState.category,

        balance = uiState.balance,
        balanceBaseCurrency = uiState.balanceBaseCurrency,
        income = uiState.income,
        expenses = uiState.expenses,

        initWithTransactions = uiState.initWithTransactions,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,

        history = uiState.history,
        shouldShowAccountSpecificColorInTransactions = uiState.showAccountColorsInTransactions,
        compactTransactions = uiState.compactTransactions,

        upcoming = uiState.upcoming,
        upcomingExpanded = uiState.upcomingExpanded,
        setUpcomingExpanded = {
            viewModel.onEvent(TransactionsEvent.SetUpcomingExpanded(it))
        },
        upcomingIncome = uiState.upcomingIncome,
        upcomingExpenses = uiState.upcomingExpenses,

        overdue = uiState.overdue,
        overdueExpanded = uiState.overdueExpanded,
        setOverdueExpanded = {
            viewModel.onEvent(TransactionsEvent.SetOverdueExpanded(it))
        },
        overdueIncome = uiState.overdueIncome,
        overdueExpenses = uiState.overdueExpenses,

        onSetPeriod = {
            viewModel.onEvent(
                TransactionsEvent.SetPeriod(
                    screen = screen,
                    period = it
                )
            )
        },
        onNextMonth = {
            viewModel.onEvent(TransactionsEvent.NextMonth(screen))
        },
        onPreviousMonth = {
            viewModel.onEvent(TransactionsEvent.PreviousMonth(screen))
        },
        onDelete = {
            viewModel.onEvent(TransactionsEvent.Delete(screen))
        },
        onEditCategory = {
            viewModel.onEvent(TransactionsEvent.EditCategory(it))
        },
        onEditAccount = { acc, newBalance, creditCards ->
            viewModel.onEvent(TransactionsEvent.EditAccount(screen, acc, newBalance, creditCards))
        },
        onPayOrGet = { transaction ->
            viewModel.onEvent(TransactionsEvent.PayOrGet(screen, transaction))
        },
        onSkipTransaction = { transaction ->
            viewModel.onEvent(TransactionsEvent.SkipTransaction(screen, transaction))
        },
        onSkipAllTransactions = { transactions ->
            viewModel.onEvent(TransactionsEvent.SkipTransactions(screen, transactions))
        },
        updateAccountNameConfirmation = {
            viewModel.onEvent(TransactionsEvent.UpdateAccountDeletionState(it))
        },
        enableDeletionButton = uiState.enableDeletionButton,
        skipAllModalVisible = uiState.skipAllModalVisible,
        onSkipAllModalVisible = {
            viewModel.onEvent(TransactionsEvent.SetSkipAllModalVisible(it))
        },
        deleteModal1Visible = uiState.deleteModal1Visible,
        onDeleteModal1Visible = {
            viewModel.onEvent(TransactionsEvent.OnDeleteModal1Visible(it))
        },
        onChoosePeriodModal = {
            viewModel.onEvent(TransactionsEvent.OnChoosePeriodModalData(it))
        },
        choosePeriodModal = uiState.choosePeriodModal,
        investment = uiState.investment,
        setValuesExpanded = {
            viewModel.onEvent(TransactionsEvent.SetValuesExpanded(it))
        },
        onSaveValue = {
            viewModel.onEvent(TransactionsEvent.SaveValue(screen, it))
        },
        onDeleteValue = {
            viewModel.onEvent(TransactionsEvent.DeleteValue(screen, it))
        },
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun BoxWithConstraintsScope.UI(
    screen: TransactionsScreen,
    period: TimePeriod,
    baseCurrency: String,
    currency: String,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,

    account: Account?,
    category: Category?,

    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,

    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,

    balance: Double,
    balanceBaseCurrency: Double?,
    income: Double,
    expenses: Double,
    choosePeriodModal: ChoosePeriodModalData?,

    history: ImmutableList<TransactionHistoryItem>,
    shouldShowAccountSpecificColorInTransactions: Boolean,

    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetPeriod: (TimePeriod) -> Unit,
    onEditAccount: (Account, Double, List<CreditCard>?) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDelete: () -> Unit,
    deleteModal1Visible: Boolean,
    onDeleteModal1Visible: (Boolean) -> Unit,

    initWithTransactions: Boolean = false,
    treatTransfersAsIncomeExpense: Boolean = false,
    upcomingExpanded: Boolean = true,
    setUpcomingExpanded: (Boolean) -> Unit = {},
    upcomingIncome: Double = 0.0,
    upcomingExpenses: Double = 0.0,
    upcoming: ImmutableList<Transaction> = persistentListOf(),

    overdueExpanded: Boolean = true,
    setOverdueExpanded: (Boolean) -> Unit = {},
    overdueIncome: Double = 0.0,
    overdueExpenses: Double = 0.0,
    overdue: ImmutableList<Transaction> = persistentListOf(),

    onPayOrGet: (Transaction) -> Unit = {},
    onSkipTransaction: (Transaction) -> Unit = {},
    onSkipAllTransactions: (List<Transaction>) -> Unit = {},
    compactTransactions: Boolean = false,
    creditCards: ImmutableList<CreditCard> = persistentListOf(),
    investment: InvestmentData? = null,
    setValuesExpanded: (Boolean) -> Unit = {},
    onSaveValue: (AccountValue) -> Unit = {},
    onDeleteValue: (AccountValue) -> Unit = {},
    onChoosePeriodModal: (ChoosePeriodModalData?) -> Unit,
) {
    val periodState = LocalPeriodState.current
    val screenSize = LocalScreenSize.current
    val itemColor = account?.let { accountColor(it.color.value) }
        ?: category?.let { categoryColor(it.color.value) } ?: Gray

    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var valueModalData: AccountValueModalData? by remember { mutableStateOf(null) }
    // An investment account's value is entered, rather than adjusted by a transaction.
    val updateValue: (() -> Unit)? = account?.takeIf { investment != null }?.let { investmentAccount ->
        {
            valueModalData = AccountValueModalData(value = null, account = investmentAccount.id, currency = currency)
        }
    }

    val swipeListenerState = rememberSwipeListenerState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(itemColor)
            .thenIf(!initWithTransactions) {
                horizontalSwipeListener(
                    sensitivity = 150,
                    state = swipeListenerState,
                    onSwipeLeft = {
                        onNextMonth()
                    },
                    onSwipeRight = {
                        onPreviousMonth()
                    }
                )
            }
    ) {
        val listState = rememberScrollPositionListState(
            key = "item_stats_lazy_column",
            screen = screen
        )
        val valuesShown = investment?.values?.isNotEmpty() == true
        val surface = MaterialTheme.colorScheme.surface
        val density = LocalDensity.current

        val timeProvider = LocalTimeProvider.current
        val timeConverter = LocalTimeConverter.current
        val timeFormatter = LocalTimeFormatter.current
        val emptyStateTitle = stringResource(R.string.no_transactions)
        val emptyStateText = stringResource(
            R.string.no_transactions_for_period,
            period.toDisplayLong(
                startDateOfMonth = periodState.startDayOfMonth,
                timeProvider = timeProvider,
                timeConverter = timeConverter,
                timeFormatter = timeFormatter,
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp)
                .clip(ShapeTop)
                .surfaceBelowHeader(listState, MaterialTheme.colorScheme.surface)
                .testTag("item_stats_lazy_column"),
            state = listState,
        ) {
            item {
                Header(
                    screen = screen,
                    history = history,
                    income = income,
                    expenses = expenses,
                    currency = currency,
                    baseCurrency = baseCurrency,
                    itemColor = itemColor,
                    account = account,
                    category = category,
                    balance = balance,
                    balanceBaseCurrency = balanceBaseCurrency,
                    treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense,
                    investment = investment,
                    onUpdateValue = updateValue,

                    onDelete = {
                        onDeleteModal1Visible(true)
                    },
                    onEdit = {
                        when {
                            account != null -> {
                                accountModalData = AccountModalData(
                                    account = account,
                                    creditCards = creditCards,
                                    baseCurrency = currency,
                                    balance = balance,
                                    autoFocusKeyboard = false
                                )
                            }

                            category != null -> {
                                categoryModalData = CategoryModalData(
                                    category = category,
                                    autoFocusKeyboard = false
                                )
                            }
                        }
                    },

                    onBalanceClick = {
                        when {
                            updateValue != null -> updateValue()
                            account != null -> {
                                accountModalData = AccountModalData(
                                    account = account,
                                    creditCards = creditCards,
                                    baseCurrency = currency,
                                    balance = balance,
                                    adjustBalanceMode = true,
                                    autoFocusKeyboard = false
                                )
                            }
                        }
                    },
                    showCategoryModal = {
                        categoryModalData = CategoryModalData(
                            category = category,
                            autoFocusKeyboard = false
                        )
                    },
                    showAccountModal = {
                        accountModalData = AccountModalData(
                            account = account,
                            creditCards = creditCards,
                            baseCurrency = currency,
                            balance = balance,
                            adjustBalanceMode = false,
                            autoFocusKeyboard = false
                        )
                    }
                )
            }

            if (investment != null && account != null && valuesShown) {
                item(key = "values_section") {
                    Box {
                        RoundedTopJoin(itemColor)

                        Column {
                            // With two months or more there's a line to draw.
                            if (investment.chart.size > 1) {
                                Spacer(Modifier.height(24.dp))
                                ValueChart(points = investment.chart)
                            }

                            SectionDivider(
                                expanded = investment.valuesExpanded,
                                title = stringResource(R.string.value_updates),
                                titleColor = Nivy,
                                baseCurrency = baseCurrency,
                                income = 0.0,
                                expenses = 0.0,
                                showIncomeExpenseRow = false,
                                setExpanded = setValuesExpanded,
                            )
                        }
                    }
                }
                if (investment.valuesExpanded) {
                    items(investment.values, key = { it.value.id.value }) { entry ->
                        Spacer(Modifier.height(12.dp))
                        ValueEntryRow(entry = entry, currency = currency) {
                            valueModalData = AccountValueModalData(
                                value = entry.value,
                                account = account.id,
                                currency = currency,
                            )
                        }
                    }
                }
            }

            choosePeriodModal(
                period = period,
                // After the values the surface has started: no header color behind its corners.
                itemColor = if (valuesShown) surface else itemColor,
                initWithTransactions = initWithTransactions,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onChoosePeriodModal = onChoosePeriodModal
            )

            transactions(
                baseData = AppBaseData(
                    baseCurrency,
                    accounts,
                    categories
                ),
                upcoming = DueSection(
                    trns = upcoming,
                    stats = IncomeExpensePair(
                        income = upcomingIncome.toBigDecimal(),
                        expense = upcomingExpenses.toBigDecimal()
                    ),
                    expanded = upcomingExpanded
                ),
                setUpcomingExpanded = setUpcomingExpanded,

                overdue = DueSection(
                    trns = overdue,
                    stats = IncomeExpensePair(
                        income = overdueIncome.toBigDecimal(),
                        expense = overdueExpenses.toBigDecimal()
                    ),
                    expanded = overdueExpanded
                ),
                setOverdueExpanded = setOverdueExpanded,

                history = history,
                lastItemSpacer = with(density) {
                    (screenSize.height * 0.7f).toDp()
                },

                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction,
                onSkipAllTransactions = {
                    onSkipAllModalVisible(true)
                },
                emptyStateTitle = emptyStateTitle,
                emptyStateText = emptyStateText,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                compact = compactTransactions,
            )
        }
    }

    DeleteModals(
        account = account,
        category = category,
        updateAccountNameConfirmation = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        onDelete = onDelete,
        skipAllModalVisible = skipAllModalVisible,
        onSkipAllModalVisible = {
            onSkipAllModalVisible(it)
        },
        onSkipAllTransactions = onSkipAllTransactions,
        overdue = overdue,
        deleteModal1Visible = deleteModal1Visible,
        setDeleteModal1Visible = onDeleteModal1Visible
    )

    CategoryModal(
        modal = categoryModalData,
        onCreateCategory = { },
        onEditCategory = onEditCategory,
        dismiss = {
            categoryModalData = null
        }
    )

    AccountModal(
        modal = accountModalData,
        onCreateAccount = { },
        onEditAccount = onEditAccount,
        dismiss = {
            accountModalData = null
        }
    )

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            onChoosePeriodModal(null)
        }
    ) {
        onSetPeriod(it)
    }

    AccountValueModal(
        modal = valueModalData,
        onSave = onSaveValue,
        onDelete = onDeleteValue,
        dismiss = {
            valueModalData = null
        }
    )
}

// Rounded corners top effect: the list's surface starts under the header, its corners over the
// header's color.
@Composable
internal fun RoundedTopJoin(itemColor: Color) {
    Spacer(
        Modifier
            .height(32.dp)
            .fillMaxWidth()
            .background(itemColor) // itemColor is displayed below the clip
            .background(MaterialTheme.colorScheme.surface, ShapeTop)
    )
}

/**
 * The list's background, below its header (the first item) while it shows: under the header, the
 * rounded corners would cut the background's edge and the header's alike, and the background would
 * show through as a faint arc.
 */
private fun Modifier.surfaceBelowHeader(listState: LazyListState, color: Color) = drawBehind {
    val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
    val top = header?.let { (it.offset + it.size).toFloat() } ?: 0f
    drawRect(color, topLeft = Offset(0f, top))
}
