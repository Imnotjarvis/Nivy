package com.ivy.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.Theme
import com.ivy.base.model.TransactionType
import com.ivy.base.time.impl.DeviceTimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.design.system.colors.isLight
import com.ivy.design.utils.LocalScreenSize
import com.ivy.design.utils.thenIf
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.LocalPeriodState
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.DueSection
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ui.component.IncomeExpensesCards
import com.ivy.legacy.ui.component.ItemStatisticToolbar
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.legacy.ui.previewAccount
import com.ivy.legacy.ui.previewTransaction
import com.ivy.legacy.utils.balancePrefix
import com.ivy.design.utils.clickableNoIndication
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.design.utils.StatusBarDarkText
import com.ivy.design.utils.setStatusBarDarkTextCompat
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.legacy.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.GreenDark
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMedium
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.design.system.colors.dynamicContrast
import com.ivy.design.system.colors.findContrastTextColor
import com.ivy.design.system.colors.isDarkColor
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.wallet.ui.theme.modal.DeleteConfirmationModal
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.design.system.colors.toComposeColor
import com.ivy.wallet.ui.theme.wallet.PeriodSelector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.shapes.ShapeTop32
import androidx.compose.runtime.DisposableEffect

@Composable
fun BoxWithConstraintsScope.TransactionsScreen(screen: TransactionsScreen) {
    val viewModel: TransactionsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    val view = LocalView.current
    LaunchedEffect(Unit) {
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
        category = uiState.category,

        balance = uiState.balance,
        balanceBaseCurrency = uiState.balanceBaseCurrency,
        income = uiState.income,
        expenses = uiState.expenses,

        initWithTransactions = uiState.initWithTransactions,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,

        history = uiState.history,
        shouldShowAccountSpecificColorInTransactions = uiState.showAccountColorsInTransactions,

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
        onEditAccount = { acc, newBalance ->
            viewModel.onEvent(TransactionsEvent.EditAccount(screen, acc, newBalance))
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
        choosePeriodModal = uiState.choosePeriodModal
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun BoxWithConstraintsScope.UI(
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
    onEditAccount: (Account, Double) -> Unit,
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
    onChoosePeriodModal: (ChoosePeriodModalData?) -> Unit,
) {
    val periodState = LocalPeriodState.current
    val screenSize = LocalScreenSize.current
    val itemColor = (account?.color?.value ?: category?.color?.value)?.toComposeColor() ?: Gray

    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

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
            key = "item_stats_lazy_column"
        )
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
                .clip(ShapeTop32)
                .background(MaterialTheme.colorScheme.surface)
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

                    onDelete = {
                        onDeleteModal1Visible(true)
                    },
                    onEdit = {
                        when {
                            account != null -> {
                                accountModalData = AccountModalData(
                                    account = account,
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
                            account != null -> {
                                accountModalData = AccountModalData(
                                    account = account,
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
                            baseCurrency = currency,
                            balance = balance,
                            adjustBalanceMode = false,
                            autoFocusKeyboard = false
                        )
                    }
                )
            }

            choosePeriodModal(
                period = period,
                itemColor = itemColor,
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
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions
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
}

private fun LazyListScope.choosePeriodModal(
    period: TimePeriod,
    itemColor: Color,
    initWithTransactions: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onChoosePeriodModal: (ChoosePeriodModalData?) -> Unit,
) {
    item {
        // Rounded corners top effect
        Box {
            Spacer(
                Modifier
                    .height(32.dp)
                    .fillMaxWidth()
                    .background(itemColor) // itemColor is displayed below the clip
                    .background(MaterialTheme.colorScheme.surface, ShapeTop32)
            )

            PeriodSelector(
                modifier = Modifier.padding(top = 16.dp),
                period = period,
                onPreviousMonth = { if (!initWithTransactions) onPreviousMonth() },
                onNextMonth = { if (!initWithTransactions) onNextMonth() },
                onShowChoosePeriodModal = {
                    if (!initWithTransactions) {
                        onChoosePeriodModal(
                            ChoosePeriodModalData(
                                period = period
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.DeleteModals(
    deleteModal1Visible: Boolean,
    setDeleteModal1Visible: (Boolean) -> Unit,
    account: Account?,
    category: Category?,
    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,
    onDelete: () -> Unit,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    overdue: ImmutableList<Transaction> = persistentListOf(),
) {
    var deleteModal3Visible by remember { mutableStateOf(false) }

    DeleteModal(
        visible = deleteModal1Visible,
        title = stringResource(R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(R.string.account_confirm_deletion_description)
        } else {
            stringResource(R.string.category_confirm_deletion_description)
        },
        dismiss = {
            setDeleteModal1Visible(false)
        }
    ) {
        deleteModal3Visible = true
    }

    DeleteConfirmationModal(
        visible = deleteModal3Visible,
        title = stringResource(id = R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(
                id = R.string.account_confirm_deletion_type_account_name,
                account.name.value
            )
        } else {
            stringResource(R.string.please_type_category_name, category?.name?.value ?: "")
        },
        hint = if (account != null) stringResource(id = R.string.account_name) else "Category name",
        onAccountNameChange = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        dismiss = {
            updateAccountNameConfirmation("")
            deleteModal3Visible = false
            setDeleteModal1Visible(false)
        }
    ) {
        onDelete()
        updateAccountNameConfirmation("")
        setDeleteModal1Visible(false)
    }

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            onSkipAllModalVisible(false)
        }
    ) {
        onSkipAllTransactions(overdue)
        onSkipAllModalVisible(false)
    }
}

@Suppress("LongParameterList")
@Composable
private fun Header(
    screen: TransactionsScreen,
    history: ImmutableList<TransactionHistoryItem>,
    currency: String,
    baseCurrency: String,
    itemColor: Color,
    account: Account?,
    category: Category?,
    balance: Double,
    balanceBaseCurrency: Double?,
    income: Double,
    expenses: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,

    onBalanceClick: () -> Unit,
    showCategoryModal: () -> Unit,
    showAccountModal: () -> Unit,
    treatTransfersAsIncomeExpense: Boolean = false,
) {
    val contrastColor = findContrastTextColor(itemColor)

    val darkColor = isDarkColor(itemColor)
    StatusBarDarkText(darkText = !darkColor)

    Column(
        modifier = Modifier.background(itemColor)
    ) {
        Spacer(Modifier.height(20.dp))

        val hideEditAndDeleteButtonForAccountTransfer =
            screen.transactions.none { it is Transfer }

        ItemStatisticToolbar(
            contrastColor = contrastColor,
            onEdit = onEdit,
            onDelete = onDelete,
            showEditButton = hideEditAndDeleteButtonForAccountTransfer,
            showDeleteButton = hideEditAndDeleteButtonForAccountTransfer,
        )

        Spacer(Modifier.height(24.dp))

        Item(
            contrastColor = contrastColor,
            account = account,
            category = category,
            showAccountModal = showAccountModal,
            showCategoryModal = showCategoryModal
        )

        BalanceRow(
            modifier = Modifier
                .padding(start = 32.dp)
                .testTag("balance")
                .clickableNoIndication(rememberInteractionSource()) {
                    onBalanceClick()
                },
            textColor = contrastColor,
            currency = currency,
            balance = balance,
            balanceAmountPrefix = if (category != null) {
                balancePrefix(
                    income = income,
                    expenses = expenses
                )
            } else {
                null
            }
        )

        if (currency != baseCurrency && balanceBaseCurrency != null) {
            BalanceRowMedium(
                modifier = Modifier
                    .padding(start = 32.dp)
                    .clickableNoIndication(rememberInteractionSource()) {
                        onBalanceClick()
                    },
                textColor = itemColor.dynamicContrast(),
                currency = baseCurrency,
                balance = balanceBaseCurrency,
                balanceAmountPrefix = if (category != null) {
                    balancePrefix(
                        income = income,
                        expenses = expenses
                    )
                } else {
                    null
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        val nav = navigation()
        IncomeExpensesCards(
            history = history,
            currency = currency,
            income = income,
            expenses = expenses,

            hasAddButtons = true,

            itemColor = itemColor,
            incomeHeaderCardClicked = {
                if (account != null) {
                    nav.navigateTo(
                        PieChartStatisticScreen(
                            type = TransactionType.INCOME,
                            accountList = persistentListOf(account.id.value),
                            filterExcluded = false,
                            treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                        )
                    )
                }
            },
            expenseHeaderCardClicked = {
                if (account != null) {
                    nav.navigateTo(
                        PieChartStatisticScreen(
                            type = TransactionType.EXPENSE,
                            accountList = persistentListOf(account.id.value),
                            filterExcluded = false,
                            treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                        )
                    )
                }
            }
        ) { trnType ->
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = trnType,
                    accountId = account?.id?.value,
                    categoryId = category?.id?.value
                )
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Item(
    contrastColor: Color,
    account: Account?,
    category: Category?,

    showCategoryModal: () -> Unit,
    showAccountModal: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 22.dp)
            .clickableNoIndication(rememberInteractionSource()) {
                when {
                    account != null -> {
                        showAccountModal()
                    }

                    category != null -> {
                        showCategoryModal()
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            account != null -> {
                ItemIconMDefaultIcon(
                    iconName = account.icon?.id,
                    defaultIcon = R.drawable.ic_custom_account_m,
                    tint = contrastColor
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = account.name.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                if (!account.includeInBalance) {
                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.excluded),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = account.color.value.toComposeColor().dynamicContrast(),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            category != null -> {
                ItemIconMDefaultIcon(
                    iconName = category.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_m,
                    tint = contrastColor
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = category.name.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            else -> {
                // Unspecified
                ItemIconMDefaultIcon(
                    iconName = null,
                    defaultIcon = R.drawable.ic_custom_category_m,
                    tint = contrastColor
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = Constants.CATEGORY_UNSPECIFIED_NAME,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_empty() {
    IvyPreview {
        UI(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            currency = "BGN",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = null,
            income = 8000.0,
            expenses = 6000.0,

            history = persistentListOf(),
            category = null,
            account = previewAccount("DSK", color = GreenDark, icon = "pet"),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _ -> },
            onEditCategory = {},
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_crypto() {
    IvyPreview {
        UI(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            currency = "ADA",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = 2879.28,
            income = 8000.0,
            expenses = 6000.0,

            history = persistentListOf(),
            category = null,
            account = previewAccount("DSK", color = GreenDark, icon = "pet", includeInBalance = false),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _ -> },
            onEditCategory = {},
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_empty_upcoming() {
    IvyPreview {
        UI(
            period = TimePeriod(month = Month.monthsList().first(), year = 2023),
            baseCurrency = "BGN",
            currency = "BGN",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = null,
            income = 8000.0,
            expenses = 6000.0,

            history = persistentListOf(),
            category = null,
            account = previewAccount("DSK", color = GreenDark, icon = "pet"),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _ -> },
            onEditCategory = {},
            upcoming = persistentListOf(
                previewTransaction(
                    type = TransactionType.EXPENSE,
                    account = previewAccount("Cash", color = GreenDark, currency = "BGN", id = UUID(1L, 2L)),
                    amount = 10.0,
                )
            ),
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        Preview_empty_upcoming()
    }
}