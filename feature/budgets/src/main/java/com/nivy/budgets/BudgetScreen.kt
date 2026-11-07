package com.nivy.budgets

import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.NivyPreview
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.CategoryId
import com.nivy.data.model.BudgetId
import com.nivy.base.model.Theme
import java.util.UUID
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import com.nivy.wallet.ui.theme.modal.DeleteModal
import com.nivy.wallet.LocalPeriodState
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.navigation.BudgetPage
import com.nivy.navigation.BudgetScreen
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.design.system.shapes.PillShape
import com.nivy.design.system.colors.deleteRed
import com.nivy.data.model.Budget
import com.nivy.base.model.TransactionType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.annotation.DrawableRes
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.data.model.pickable
import com.nivy.budgets.model.DisplayBudget
import com.nivy.wallet.ui.theme.wallet.PeriodSelector
import com.nivy.wallet.ui.theme.components.BudgetBattery
import com.nivy.wallet.ui.theme.components.EmptyState
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.design.system.colors.Gray
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.ReorderButton
import com.nivy.wallet.ui.theme.components.ReorderModalSingleType
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB1
import androidx.compose.material3.MaterialTheme

@Composable
fun BoxWithConstraintsScope.BudgetScreen(screen: BudgetScreen) {
    val viewModel: BudgetViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    LaunchedEffect(screen) {
        viewModel.onEvent(BudgetScreenEvent.OnOpen(screen))
    }

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: BudgetScreenState,
    onEvent: (BudgetScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    var deletingBudget by remember { mutableStateOf<Budget?>(null) }
    var savingsGoalModalVisible by remember { mutableStateOf(false) }
    val month = state.period?.toDisplayLong(
        startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
        timeProvider = LocalTimeProvider.current,
        timeConverter = LocalTimeConverter.current,
        timeFormatter = LocalTimeFormatter.current,
    ).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(32.dp))

        Toolbar(
            timeRange = state.timeRange,
            setReorderModalVisible = {
                onEvent(BudgetScreenEvent.OnReorderModalVisible(it))
            }
        )

        if (state.period != null) {
            Spacer(Modifier.height(16.dp))

            // Budgets are monthly, so there's no other period to choose.
            PeriodSelector(
                period = state.period,
                onPreviousMonth = { onEvent(BudgetScreenEvent.OnPreviousMonth) },
                onNextMonth = { onEvent(BudgetScreenEvent.OnNextMonth) },
                onShowChoosePeriodModal = {},
            )
        }

        Spacer(Modifier.height(24.dp))

        SavingsGoalItem(
            savings = state.savings,
            currency = state.baseCurrency,
        ) {
            savingsGoalModalVisible = true
        }

        for (item in state.budgets) {
            Spacer(Modifier.height(24.dp))

            BudgetItem(
                displayBudget = item,
                baseCurrency = state.baseCurrency,
                onEdit = {
                    onEvent(
                        BudgetScreenEvent.OnBudgetModalData(
                            BudgetModalData(
                                budget = item.budget,
                                baseCurrency = state.baseCurrency,
                                categories = state.categories,
                                accounts = state.accounts.pickable(item.budget.accounts),
                                autoFocusKeyboard = false
                            )
                        )
                    )
                },
                onDelete = { deletingBudget = item.budget },
            ) {
                state.timeRange?.let { nav.navigateTo(budgetPage(item, month, it.toCloseTimeRange())) }
            }
        }

        if (state.budgets.isEmpty()) {
            Spacer(Modifier.weight(1f))

            EmptyState(
                icon = R.drawable.ic_budget_xl,
                title = stringResource(R.string.no_budgets),
                text = stringResource(R.string.no_budgets_text)
            )

            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(150.dp)) // scroll hack
    }

    BudgetBottomBar(
        onAdd = {
            onEvent(
                BudgetScreenEvent.OnBudgetModalData(
                    BudgetModalData(
                        budget = null,
                        baseCurrency = state.baseCurrency,
                        categories = state.categories,
                        accounts = state.accounts.pickable(),
                    )
                )
            )
        },
        onClose = {
            nav.back()
        },
    )

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = state.budgets,
        dismiss = {
            onEvent(BudgetScreenEvent.OnReorderModalVisible(false))
        },
        onReordered = { onEvent(BudgetScreenEvent.OnReorder(it)) }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.budget.name.value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
    }

    DeleteModal(
        visible = deletingBudget != null,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(
            R.string.confirm_budget_deletion_warning,
            deletingBudget?.name?.value.orEmpty()
        ),
        dismiss = { deletingBudget = null }
    ) {
        deletingBudget?.let { onEvent(BudgetScreenEvent.OnDeleteBudget(it)) }
        deletingBudget = null
    }

    val savingsGoalModalId = remember { UUID.randomUUID() }
    AmountModal(
        id = savingsGoalModalId,
        visible = savingsGoalModalVisible,
        currency = state.baseCurrency,
        initialAmount = state.savings.goal.takeIf { it > 0 },
        dismiss = { savingsGoalModalVisible = false }
    ) {
        onEvent(BudgetScreenEvent.OnSetSavingsGoal(it))
    }

    BudgetModal(
        modal = state.budgetModalData,
        onCreate = { name, amount, categories, accounts ->
            onEvent(BudgetScreenEvent.OnCreateBudget(name, amount, categories, accounts))
        },
        onEdit = { onEvent(BudgetScreenEvent.OnEditBudget(it)) },
        onDelete = { onEvent(BudgetScreenEvent.OnDeleteBudget(it)) },
        dismiss = {
            onEvent(BudgetScreenEvent.OnBudgetModalData(null))
        }
    )
}

@Composable
private fun Toolbar(
    timeRange: com.nivy.wallet.data.model.FromToTimeRange?,
    setReorderModalVisible: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.budgets),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (timeRange != null) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = timeRange.toDisplay(LocalTimeFormatter.current),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        ReorderButton {
            setReorderModalVisible(true)
        }

        Spacer(Modifier.width(24.dp))
    }
}

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
private fun BudgetItem(
    displayBudget: DisplayBudget,
    baseCurrency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(rememberInteractionSource()) {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = displayBudget.budget.name.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = determineBudgetType(displayBudget.budget.categories.size),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Gray,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        AmountCurrencyB1(
            amount = displayBudget.budget.amount.value,
            currency = baseCurrency,
            amountFontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.width(32.dp))
    }

    Spacer(Modifier.height(12.dp))

    BudgetBattery(
        modifier = Modifier.padding(horizontal = 16.dp),
        currency = baseCurrency,
        expenses = displayBudget.spentAmount,
        budget = displayBudget.budget.amount.value,
        backgroundNotFilled = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        trailing = { tint -> BudgetMenu(tint = tint, onEdit = onEdit, onDelete = onDelete) },
    )
}

/** The budget's ⋮: Edit and Delete, in [tint], which reads on the bar under it. */
@Composable
private fun BudgetMenu(
    tint: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        NivyIcon(
            modifier = Modifier
                .clip(PillShape)
                .clickable { expanded = true }
                .padding(4.dp)
                .testTag("budget_menu"),
            icon = R.drawable.ic_more_vertical,
            tint = tint,
            contentDescription = stringResource(R.string.more_options),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            BudgetMenuItem(
                icon = R.drawable.ic_edit,
                text = stringResource(R.string.edit),
                color = MaterialTheme.colorScheme.onSurface,
            ) {
                expanded = false
                onEdit()
            }
            BudgetMenuItem(
                icon = R.drawable.ic_delete,
                text = stringResource(R.string.delete),
                color = deleteRed(),
            ) {
                expanded = false
                onDelete()
            }
        }
    }
}

@Composable
private fun BudgetMenuItem(
    @DrawableRes icon: Int,
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(color = color, fontWeight = FontWeight.SemiBold)
            )
        },
        leadingIcon = { NivyIcon(icon = icon, tint = color) },
        onClick = onClick,
    )
}

/** The budget's page: the pie chart of its expenses in [month], which [range] covers, listed under it. */
private fun budgetPage(item: DisplayBudget, month: String, range: ClosedTimeRange) = PieChartStatisticScreen(
    type = TransactionType.EXPENSE,
    budgetPage = BudgetPage(budget = item.budget, month = month, range = range),
)

/** For screenshot testing: a month with a savings goal, budgets within their amounts and one over. */
@Composable
fun BudgetScreenUiTest(isDark: Boolean) {
    NivyPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        UI(
            state = BudgetScreenState(
                baseCurrency = "EUR",
                budgets = persistentListOf(
                    sampleBudget("Groceries", amount = 400.0, spent = 318.4, categories = 1),
                    sampleBudget("Eating out", amount = 150.0, spent = 172.5, categories = 1),
                    sampleBudget("Everything", amount = 2000.0, spent = 1240.0, categories = 0),
                    sampleBudget("Transport", amount = 120.0, spent = 30.0, categories = 2),
                ),
                categories = persistentListOf(),
                accounts = persistentListOf(),
                savings = SavingsGoal(goal = 500.0, saved = 320.0),
                timeRange = FromToTimeRange(
                    from = Instant.parse("2023-11-01T00:00:00Z"),
                    to = Instant.parse("2023-11-30T23:59:59Z"),
                ),
                period = TimePeriod(month = Month.fromMonthValue(11), year = 2023),
                reorderModalVisible = false,
                budgetModalData = null,
            )
        )
    }
}

private fun sampleBudget(name: String, amount: Double, spent: Double, categories: Int) = DisplayBudget(
    budget = Budget(
        id = BudgetId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        amount = PositiveDouble.unsafe(amount),
        categories = List(categories) { CategoryId(UUID.randomUUID()) },
        accounts = emptyList(),
        orderNum = 0.0,
    ),
    spentAmount = spent,
)
