package com.nivy.planned.list

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.base.model.IntervalType
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Purple
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewPlannedPaymentRule
import com.nivy.navigation.EditPlannedScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.ui.rememberScrollPositionListState
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.Orange
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID
import androidx.compose.material3.MaterialTheme

@Composable
fun BoxWithConstraintsScope.PlannedPaymentsScreen() {
    val viewModel: PlannedPaymentsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: PlannedPaymentsScreenState,
    onEvent: (PlannedPaymentsScreenEvent) -> Unit = {}
) {
    PlannedPaymentsLazyColumn(
        header = {
            Spacer(Modifier.height(32.dp))

            Text(
                modifier = Modifier.padding(start = 24.dp),
                text = stringResource(R.string.planned_payments_inline),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(24.dp))
        },
        currency = state.currency,
        categories = state.categories,
        accounts = state.accounts,
        oneTime = state.oneTimePlannedPayment,
        oneTimeIncome = state.oneTimeIncome,
        oneTimeExpenses = state.oneTimeExpenses,
        recurring = state.recurringPlannedPayment,
        recurringIncome = state.recurringIncome,
        recurringExpenses = state.recurringExpenses,
        oneTimeExpanded = state.isOneTimePaymentsExpanded,
        recurringExpanded = state.isRecurringPaymentsExpanded,
        setOneTimeExpanded = {
            onEvent(PlannedPaymentsScreenEvent.OnOneTimePaymentsExpanded(it))
        },
        setRecurringExpanded = {
            onEvent(PlannedPaymentsScreenEvent.OnRecurringPaymentsExpanded(it))
        },
        listState = rememberScrollPositionListState(key = "plannedPayments")
    )

    val nav = navigation()
    PlannedPaymentsBottomBar(
        onClose = {
            nav.back()
        },
        onAdd = {
            nav.navigateTo(
                EditPlannedScreen(
                    type = TransactionType.EXPENSE,
                    plannedPaymentRuleId = null
                )
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        val account = previewAccount("Cash", color = Green, currency = "BGN")
        val food = Category(
            name = NotBlankTrimmedString.unsafe("Food"),
            color = ColorInt(Purple.toArgb()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
        val shisha = Category(
            name = NotBlankTrimmedString.unsafe("Shisha"),
            color = ColorInt(Orange.toArgb()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        UI(
            PlannedPaymentsScreenState(
                currency = "BGN",
                accounts = persistentListOf(account),
                categories = persistentListOf(food, shisha),
                oneTimePlannedPayment = persistentListOf(
                    previewPlannedPaymentRule(
                        account = account.id,
                        title = "Lidl pazar",
                        category = food.id,
                        amount = 250.75,
                        startDate = Instant.now().plus(5, ChronoUnit.DAYS),
                        type = TransactionType.EXPENSE,
                    )
                ),
                oneTimeExpenses = 250.75,
                oneTimeIncome = 0.0,
                recurringPlannedPayment = persistentListOf(
                    previewPlannedPaymentRule(
                        account = account.id,
                        title = "Tabu",
                        category = shisha.id,
                        amount = 1025.5,
                        startDate = Instant.now().plus(5, ChronoUnit.DAYS),
                        type = TransactionType.EXPENSE,
                        interval = 1 to IntervalType.MONTH,
                    )
                ),
                recurringExpenses = 1025.5,
                recurringIncome = 0.0,
                isOneTimePaymentsExpanded = true,
                isRecurringPaymentsExpanded = true
            )
        )
    }
}
