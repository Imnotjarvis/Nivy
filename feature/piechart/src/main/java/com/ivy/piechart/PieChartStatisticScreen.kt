package com.ivy.piechart

import java.time.ZoneOffset
import java.time.Instant
import kotlinx.collections.immutable.toImmutableList
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.data.TransactionHistoryEntry
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.data.model.Month
import com.ivy.wallet.data.AppBaseData
import com.ivy.navigation.BudgetPage
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.TransactionId
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Expense
import com.ivy.data.model.AccountId
import com.ivy.data.model.Account
import com.ivy.base.model.Theme
import com.ivy.base.model.AccountType
import androidx.compose.foundation.lazy.LazyListScope
import com.ivy.wallet.ui.component.transaction.transactions
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.ivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.model.TransactionType
import com.ivy.base.time.impl.DeviceTimeProvider
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.wallet.LocalPeriodState
import com.ivy.design.system.typography.numberHeadlineLarge
import com.ivy.wallet.utils.format
import androidx.compose.foundation.text.TextAutoSize
import com.ivy.wallet.utils.shortenAmount
import com.ivy.wallet.utils.horizontalSwipeListener
import com.ivy.wallet.utils.rememberSwipeListenerState
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.design.system.colors.ColorMode
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.IvyDark
import com.ivy.design.system.colors.IvyLight
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.Red
import com.ivy.design.system.colors.RedLight
import com.ivy.design.system.colors.White
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.wallet.ui.theme.components.ItemIconM
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.design.system.colors.readableOn
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.design.system.colors.toComposeColor
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1Row
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge

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
        onEvent = viewModel::onEvent
    )
}

@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    state: PieChartStatisticState,
    onEvent: (PieChartStatisticEvent) -> Unit = {}
) {
    val nav = navigation()
    val lazyState = rememberScrollPositionListState(
        key = "item_pie_chart_lazy_column"
    )
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

@Composable
private fun Header(
    transactionType: TransactionType,
    period: com.ivy.wallet.data.model.TimePeriod,
    percentExpanded: Float,

    currency: String,
    amount: Double,

    onShowMonthModal: () -> Unit,
    onSelectNextMonth: () -> Unit,
    onSelectPreviousMonth: () -> Unit,

    onClose: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    showCloseButtonOnly: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Solid, in the page's own color: not a lighter shade over it.
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            // Like Home's header: room below the row too, so scrolled content stays clear of it.
            .padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        CloseButton {
            onClose()
        }

        Spacer(Modifier.width(12.dp))

        // The amount takes the room the other items leave, so it never runs into the month
        // selector.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (percentExpanded < 1f) {
                FittingBalanceMini(
                    modifier = Modifier.alpha(1f - percentExpanded),
                    currency = currency,
                    amount = amount,
                )
            }
        }

        if (!showCloseButtonOnly) {
            Spacer(Modifier.width(16.dp))

            IvyOutlinedButton(
                modifier = Modifier.horizontalSwipeListener(
                    sensitivity = 75,
                    state = rememberSwipeListenerState(),
                    onSwipeLeft = {
                        onSelectNextMonth()
                    },
                    onSwipeRight = {
                        onSelectPreviousMonth()
                    }
                ),
                iconStart = R.drawable.ic_calendar,
                text = period.toDisplayShort(
                    startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
                    timeConverter = LocalTimeConverter.current,
                    timeProvider = LocalTimeProvider.current,
                    timeFormatter = LocalTimeFormatter.current,
                    shortMonth = true,

                    ),
            ) {
                onShowMonthModal()
            }

            if (percentExpanded > 0f) {
                Spacer(Modifier.width(12.dp))

                val backgroundColor = if (transactionType == TransactionType.EXPENSE) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Green
                }
                CircleButtonFilled(
                    modifier = Modifier
                        .alpha(percentExpanded)
                        .size(com.ivy.design.utils.lerp(1, 40, percentExpanded).dp),
                    clickAreaPadding = 4.dp,
                    icon = R.drawable.ic_plus,
                    backgroundColor = backgroundColor,
                    tint = if (transactionType == TransactionType.EXPENSE) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        White
                    }
                ) {
                    onAdd(transactionType)
                }
            }

            Spacer(Modifier.width(20.dp))
        }
    }
}

// The amount in full when it fits the width it's given, otherwise shortened (2.69k), and
// smaller still if even that is too wide.
@Composable
private fun FittingBalanceMini(
    currency: String,
    amount: Double,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val full = subcompose("full") {
            BalanceRowMini(currency = currency, balance = amount)
        }.map { it.measure(loose) }
        val placeables = if (full.maxOf { it.width } <= constraints.maxWidth) {
            full
        } else {
            subcompose("short") {
                Text(
                    text = "$currency ${shortenAmount(amount)}",
                    style = MaterialTheme.typography.numberHeadlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 22.sp),
                )
            }.map { it.measure(constraints.copy(minWidth = 0)) }
        }
        val width = placeables.maxOf { it.width }.coerceAtMost(constraints.maxWidth)
        val height = placeables.maxOf { it.height }
        layout(width, height) {
            placeables.forEach { it.place(0, (height - it.height) / 2) }
        }
    }
}

@Composable
private fun CategoryAmountCard(
    categoryAmount: CategoryAmount,
    rank: Int,
    currency: String,
    totalAmount: Double,

    selectedCategory: SelectedCategory?,

    onClick: () -> Unit
) {
    val category = categoryAmount.category
    val amount = categoryAmount.amount

    // In minimal colors, the slice's tone: the list is the chart's legend.
    val categoryColor = if (ColorMode.minimal) {
        pieTone(rank)
    } else {
        category?.color?.value?.toComposeColor() ?: Gray // Unspecified category = Gray
    }
    val selectedState = when {
        selectedCategory == null -> {
            // no selectedCategory
            false
        }

        categoryAmount.category == selectedCategory.category -> {
            // selectedCategory && we're selected
            true
        }

        else -> false
    }
    val backgroundColor = if (selectedState) categoryColor else MaterialTheme.colorScheme.surfaceVariant

    val textColor = readableOn(backgroundColor)

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor, MaterialTheme.shapes.large)
            .clickable {
                onClick()
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        ItemIconM(
            modifier = Modifier.background(categoryColor, PillShape),
            iconName = category?.icon?.id,
            tint = readableOn(categoryColor),
            iconContentScale = ContentScale.None,
            default = {
                ItemIconMDefaultIcon(
                    modifier = Modifier.background(categoryColor, PillShape),
                    iconName = category?.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_m,
                    tint = readableOn(categoryColor)
                )
            }
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    text = category?.name?.value ?: stringResource(R.string.unspecified),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                )

                PercentText(
                    amount = amount,
                    totalAmount = totalAmount,
                    selectedState = selectedState,
                    contrastColor = textColor
                )

                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.height(4.dp))

            AmountCurrencyB1Row(
                amount = amount,
                currency = currency,
                textColor = textColor,
                amountFontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PercentText(
    amount: Double,
    totalAmount: Double,
    selectedState: Boolean,
    contrastColor: Color
) {
    Text(
        text = if (totalAmount != 0.0) {
            stringResource(R.string.percent, ((amount / totalAmount) * 100).format(2))
        } else {
            stringResource(R.string.percent, "0")
        },
        style = MaterialTheme.typography.numberBodyLarge.copy(
            color = if (selectedState) contrastColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
    )
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Expense() {
    com.ivy.wallet.IvyWalletPreview {
        val state = PieChartStatisticState(
            transactionType = TransactionType.EXPENSE,
            period = com.ivy.wallet.data.model.TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            totalAmount = 1828.0,
            categoryAmounts = persistentListOf(
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Bills"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("bills"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 791.0
                ),
                CategoryAmount(
                    category = null,
                    amount = 497.0,
                    isCategoryUnspecified = true
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("trees"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 411.93
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Food & Drink"),
                        color = ColorInt(IvyDark.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 260.03
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Gifts"),
                        color = ColorInt(RedLight.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 160.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Clothes & Jewelery Fancy"),
                        color = ColorInt(Red.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Finances, Burocracy & Governance"),
                        color = ColorInt(IvyLight.toArgb()),
                        icon = IconAsset.unsafe("work"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
            ),
            selectedCategory = null,
            accountIdFilterList = persistentListOf(),
            choosePeriodModal = null,
            filterExcluded = false,
            showCloseButtonOnly = false,
            transactions = persistentListOf()
        )

        UI(state = state)
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Income() {
    com.ivy.wallet.IvyWalletPreview {
        val state = PieChartStatisticState(
            transactionType = TransactionType.INCOME,
            period = com.ivy.wallet.data.model.TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            baseCurrency = "BGN",
            totalAmount = 1828.0,
            categoryAmounts = persistentListOf(
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Bills"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("bills"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 791.0
                ),
                CategoryAmount(
                    category = null,
                    amount = 497.0,
                    isCategoryUnspecified = true
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("trees"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 411.93
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Food & Drink"),
                        color = ColorInt(IvyDark.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 260.03
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Gifts"),
                        color = ColorInt(RedLight.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 160.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Clothes & Jewelery Fancy"),
                        color = ColorInt(Red.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
                CategoryAmount(
                    category = Category(
                        name = NotBlankTrimmedString.unsafe("Finances, Burocracy & Governance"),
                        color = ColorInt(IvyLight.toArgb()),
                        icon = IconAsset.unsafe("work"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    amount = 2.0
                ),
            ),
            selectedCategory = null,
            accountIdFilterList = persistentListOf(),
            choosePeriodModal = null,
            filterExcluded = false,
            showCloseButtonOnly = false,
            transactions = persistentListOf()
        )

        UI(state = state)
    }
}

/** For screenshot testing: a budget's page, its two categories and its transactions of the month. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetPageUiTest(isDark: Boolean) {
    val groceries = sampleCategory("Groceries", Green, "groceries")
    val eatingOut = sampleCategory("Eating out", Orange, "fooddrink")
    val card = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Card"),
        asset = AssetCode.unsafe("EUR"),
        color = ColorInt(IvyDark.toArgb()),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )
    val expenses = listOf(
        sampleExpense(card, groceries, 64.2, "Supermarket", "2023-11-12T18:20:00Z"),
        sampleExpense(card, eatingOut, 38.0, "Pizza night", "2023-11-12T20:30:00Z"),
        sampleExpense(card, groceries, 150.2, "Weekly shop", "2023-11-07T11:00:00Z"),
        sampleExpense(card, eatingOut, 66.0, "Sushi", "2023-11-03T13:15:00Z"),
    )
    val history = expenses.groupBy { it.time.atZone(ZoneOffset.UTC).toLocalDate() }.flatMap { (date, ofDay) ->
        listOf<TransactionHistoryItem>(
            TransactionHistoryDateDivider(date = date, income = 0.0, expenses = ofDay.sumOf { it.value.amount.value }),
        ) + ofDay.map { TransactionHistoryEntry(it) }
    }
    com.ivy.wallet.IvyWalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        UI(
            state = PieChartStatisticState(
                transactionType = TransactionType.EXPENSE,
                period = com.ivy.wallet.data.model.TimePeriod(month = Month.fromMonthValue(11), year = 2023),
                baseCurrency = "EUR",
                totalAmount = 318.4,
                categoryAmounts = persistentListOf(
                    CategoryAmount(category = groceries, amount = 214.4),
                    CategoryAmount(category = eatingOut, amount = 104.0),
                ),
                selectedCategory = null,
                accountIdFilterList = persistentListOf(),
                showCloseButtonOnly = true,
                filterExcluded = false,
                transactions = expenses.toImmutableList(),
                choosePeriodModal = null,
                budgetPage = BudgetPage(name = "Food", month = "November 2023"),
                history = history.toImmutableList(),
                baseData = AppBaseData(
                    baseCurrency = "EUR",
                    accounts = persistentListOf(card),
                    categories = persistentListOf(groceries, eatingOut),
                ),
            )
        )
    }
}

private fun sampleCategory(name: String, color: Color, icon: String) = Category(
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color.toArgb()),
    icon = IconAsset.unsafe(icon),
    id = CategoryId(UUID.randomUUID()),
    orderNum = 0.0,
)

private fun sampleExpense(account: Account, category: Category, amount: Double, title: String, time: String) =
    Expense(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(title),
        description = null,
        category = category.id,
        time = Instant.parse(time),
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
        account = account.id,
    )
