package com.ivy.loans.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.model.Theme
import com.ivy.data.model.LoanType
import com.ivy.design.system.typography.numberTitleSmall
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.humanReadableType
import com.ivy.wallet.ui.previewLoan
import com.ivy.wallet.utils.getDefaultFIATCurrency
import com.ivy.wallet.utils.horizontalSwipeListener
import com.ivy.wallet.utils.rememberSwipeListenerState
import com.ivy.loans.loan.Constants.SWIPE_HORIZONTAL_THRESHOLD
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.design.system.colors.Blue
import com.ivy.design.system.colors.Gray
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.ProgressBar
import com.ivy.wallet.ui.theme.components.ReorderButton
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.design.system.colors.dynamicContrast
import com.ivy.design.system.colors.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.LoanModal
import com.ivy.design.system.colors.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime
import com.ivy.design.system.shapes.ShapeTop16
import com.ivy.design.system.typography.numberBodyLarge

@Composable
fun BoxWithConstraintsScope.LoansScreen(screen: LoansScreen) {
    val viewModel: LoanViewModel = viewModel()
    val state = viewModel.uiState()
    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: LoanScreenState,
    onEventHandler: (LoanScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .horizontalSwipeListener(
                sensitivity = SWIPE_HORIZONTAL_THRESHOLD,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    onEventHandler.invoke(LoanScreenEvent.OnTabChanged(LoanTab.COMPLETED))
                },
                onSwipeRight = {
                    onEventHandler.invoke(LoanScreenEvent.OnTabChanged(LoanTab.PENDING))
                }
            )
    ) {
        Spacer(Modifier.height(32.dp))

        Toolbar(
            onDismiss = { nav.back() },
            setReorderModalVisible = { onEventHandler.invoke(LoanScreenEvent.OnReOrderModalShow(show = it)) },
            state.totalOweAmount,
            state.totalOwedAmount
        )

        Spacer(Modifier.height(8.dp))

        val scrollState = rememberScrollPositionListState(key = "loans_lazy_column")

        val loans = if (state.selectedTab == LoanTab.PENDING) {
            state.pendingLoans
        } else {
            state.completedLoans
        }

        LazyColumn(state = scrollState) {
            items(loans) { item ->
                Spacer(Modifier.height(16.dp))

                LoanItem(
                    displayLoan = item
                ) {
                    nav.navigateTo(
                        screen = LoanDetailsScreen(
                            loanId = item.loan.id.value
                        )
                    )
                }
            }

            item {
                Spacer(Modifier.height(150.dp)) // scroll hack
            }
        }

        if (loans.isEmpty()) {
            Spacer(Modifier.weight(1f))

            NoLoansEmptyState(
                emptyStateTitle = stringResource(R.string.no_loans),
                emptyStateText = stringResource(R.string.no_loans_description)
            )

            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(150.dp)) // scroll hack
    }

    LoanBottomBar(
        tab = state.selectedTab,
        selectTab = { onEventHandler.invoke(LoanScreenEvent.OnTabChanged(it)) },
        onAdd = {
            onEventHandler.invoke(LoanScreenEvent.OnAddLoan)
        }
    )

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = if (state.selectedTab == LoanTab.PENDING) state.pendingLoans else state.completedLoans,
        dismiss = {
            onEventHandler.invoke(LoanScreenEvent.OnReOrderModalShow(show = false))
        },
        onReordered = {
            onEventHandler.invoke(LoanScreenEvent.OnReordered(reorderedList = it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.loan.name.value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
    }

    if (state.loanModalData != null) {
        LoanModal(
            accounts = state.accounts,
            onCreateAccount = {
                onEventHandler.invoke(LoanScreenEvent.OnCreateAccount(accountData = it))
            },
            modal = state.loanModalData,
            onCreateLoan = {
                onEventHandler.invoke(LoanScreenEvent.OnLoanCreate(createLoanData = it))
            },
            onEditLoan = { _, _ -> },
            dismiss = {
                onEventHandler.invoke(LoanScreenEvent.OnLoanModalDismiss)
            },
            dateTime = state.dateTime,
            onSetDate = {
                onEventHandler.invoke(LoanScreenEvent.OnChangeDate)
            },
            onSetTime = {
                onEventHandler.invoke(LoanScreenEvent.OnChangeTime)
            }
        )
    }
}

@Composable
private fun Toolbar(
    onDismiss: () -> Unit,
    setReorderModalVisible: (Boolean) -> Unit,
    totalOweAmount: String,
    totalOwedAmount: String
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
                text = stringResource(R.string.loans),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            if (totalOweAmount.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.you_owe, totalOweAmount),
                    style = MaterialTheme.typography.numberTitleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (totalOwedAmount.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.you_are_owed, totalOwedAmount),
                    style = MaterialTheme.typography.numberTitleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        CircleButtonFilled(
            modifier = Modifier,
            icon = R.drawable.ic_dismiss,
            onClick = onDismiss
        )

        Spacer(Modifier.width(8.dp))

        ReorderButton {
            setReorderModalVisible(true)
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun LoanItem(
    displayLoan: DisplayLoan,
    onClick: () -> Unit
) {
    val loan = displayLoan.loan
    val contrastColor = findContrastTextColor(loan.color.value.toComposeColor())

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .testTag("loan_item")
            .clickable(
                onClick = onClick
            )
    ) {
        LoanHeader(
            displayLoan = displayLoan,
            contrastColor = contrastColor,
        )

        Spacer(Modifier.height(12.dp))

        LoanInfo(
            displayLoan = displayLoan
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LoanHeader(
    displayLoan: DisplayLoan,
    contrastColor: Color,
) {
    val loan = displayLoan.loan

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(loan.color.value.toComposeColor(), ShapeTop16)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            ItemIconSDefaultIcon(
                iconName = loan.icon?.id,
                defaultIcon = R.drawable.ic_custom_loan_s,
                tint = contrastColor
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = loan.name.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.width(8.dp))

            Text(
                text = loan.humanReadableType(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = loan.color.value.toComposeColor().dynamicContrast(),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        val leftToPay = displayLoan.loanTotalAmount - displayLoan.amountPaid
        BalanceRow(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            textColor = contrastColor,
            currency = displayLoan.currencyCode ?: getDefaultFIATCurrency().currencyCode,
            balance = leftToPay,

            balanceFontSize = 30.sp,
            currencyFontSize = 30.sp,

            currencyUpfront = false
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnScope.LoanInfo(
    displayLoan: DisplayLoan
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        text = displayLoan.formattedDisplayText,
        style = MaterialTheme.typography.numberBodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )

    Spacer(Modifier.height(12.dp))

    ProgressBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 24.dp),
        notFilledColor = MaterialTheme.colorScheme.surfaceVariant,
        percent = displayLoan.percentPaid
    )
}

@Composable
private fun NoLoansEmptyState(
    emptyStateTitle: String,
    emptyStateText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        IvyIcon(
            icon = R.drawable.ic_custom_loan_l,
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

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 20, 0, 35)

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    val state = LoanScreenState(
        baseCurrency = "BGN",
        selectedTab = LoanTab.PENDING,
        completedLoans = persistentListOf(
            DisplayLoan(
                loan = previewLoan(
                    name = "Loan 3",
                    amount = 7000.0,
                    color = Blue,
                    type = LoanType.LEND,
                    dateTime = testDateTime,
                    icon = "bank",
                ),
                loanTotalAmount = 7000.0,
                amountPaid = 8000.0,
                percentPaid = 0.8
            ),
        ),
        pendingLoans = persistentListOf(
            DisplayLoan(
                loan = previewLoan(
                    name = "Loan 3",
                    amount = 7000.0,
                    color = Blue,
                    type = LoanType.LEND,
                    dateTime = testDateTime,
                    icon = "bank",
                ),
                loanTotalAmount = 7000.0,
                amountPaid = 8000.0,
                percentPaid = 0.8
            ),
        ),
        accounts = persistentListOf(),
        totalOweAmount = "1000.00 INR",
        totalOwedAmount = "1500.0 INR",
        loanModalData = null,
        reorderModalVisible = false,
        selectedAccount = null,
        paidOffLoanVisibility = true,
        dateTime = Instant.now()
    )
    IvyWalletPreview(theme) {
        UI(
            state = state
        ) {}
    }
}

/** For screenshot testing */
@Composable
fun LoanScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
