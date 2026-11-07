package com.ivy.loans.loandetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.model.Theme
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.LoanType
import com.ivy.wallet.IvyWalletPreview
import com.ivy.wallet.ui.previewLoan
import com.ivy.wallet.ui.previewLoanRecord
import com.ivy.loans.LoanProgress
import com.ivy.loans.loan.data.DisplayLoanRecord
import com.ivy.loans.loandetails.events.DeleteLoanModalEvent
import com.ivy.loans.loandetails.events.LoanDetailsScreenEvent
import com.ivy.loans.loandetails.events.LoanModalEvent
import com.ivy.loans.loandetails.events.LoanRecordModalEvent
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.ui.R
import com.ivy.design.system.colors.Gray
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.LoanModal
import com.ivy.wallet.ui.theme.modal.LoanRecordModal
import com.ivy.wallet.ui.theme.modal.ProgressModal
import com.ivy.design.system.colors.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.shapes.ShapeTop

@Composable
fun BoxWithConstraintsScope.LoanDetailsScreen(screen: LoanDetailsScreen) {
    val viewModel: LoanDetailsViewModel = viewModel()
    viewModel.screen = screen
    val state = viewModel.uiState()

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Composable
internal fun BoxWithConstraintsScope.UI(
    state: LoanDetailsScreenState,
    onEventHandler: (LoanDetailsScreenEvent) -> Unit = {}
) {
    val itemColor = state.loan?.color?.value?.toComposeColor() ?: Gray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(itemColor)
    ) {
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp)
                .clip(ShapeTop)
                .background(MaterialTheme.colorScheme.surface),
            state = listState,
        ) {
            item {
                if (state.loan != null) {
                    Header(
                        loan = state.loan,
                        baseCurrency = state.baseCurrency,
                        progress = state.progress,
                        itemColor = itemColor,
                        selectedLoanAccount = state.selectedLoanAccount,
                        onAmountClick = {
                            onEventHandler.invoke(LoanDetailsScreenEvent.OnAmountClick)
                        },
                        onDeleteLoan = {
                            onEventHandler.invoke(
                                DeleteLoanModalEvent.OnDismissDeleteLoan(
                                    isDeleteModalVisible = true
                                )
                            )
                        },
                        onEditLoan = {
                            onEventHandler.invoke(LoanDetailsScreenEvent.OnEditLoanClick)
                        },
                        onAddRecord = {
                            onEventHandler.invoke(LoanDetailsScreenEvent.OnAddRecord)
                        }
                    )
                }
            }

            item {
                // Rounded corners top effect
                Spacer(
                    Modifier
                        .height(32.dp)
                        .fillMaxWidth()
                        .background(itemColor) // itemColor is displayed below the clip
                        .background(MaterialTheme.colorScheme.surface, ShapeTop)
                )
            }

            if (state.loan != null) {
                loanRecords(
                    loan = state.loan,
                    displayLoanRecords = state.displayLoanRecords,
                    onClick = { displayLoanRecord ->
                        onEventHandler.invoke(
                            LoanRecordModalEvent.OnClickLoanRecord(
                                displayLoanRecord
                            )
                        )
                    }
                )
                item {
                    InitialRecordItem(
                        loan = state.loan,
                        amount = state.loan.amount.value,
                        baseCurrency = state.baseCurrency,
                    )
                }
            }

            if (state.displayLoanRecords.isEmpty()) {
                item {
                    NoLoanRecordsEmptyState()
                    Spacer(Modifier.height(96.dp))
                }
            }

            item {
                // scroll hack
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    LoanModal(
        modal = state.loanModalData, onCreateLoan = {
        // do nothing
    }, onEditLoan = { loan, createLoanTransaction ->
        onEventHandler.invoke(LoanModalEvent.OnEditLoanModal(loan, createLoanTransaction))
    }, dismiss = {
        onEventHandler.invoke(LoanModalEvent.OnDismissLoanModal)
    }, onCreateAccount = { createAccountData ->
        onEventHandler.invoke(LoanDetailsScreenEvent.OnCreateAccount(createAccountData))
    }, accounts = state.accounts, onPerformCalculations = {
        onEventHandler.invoke(LoanModalEvent.PerformCalculation)
    }, dateTime = state.dateTime,
        onSetDate = {
            onEventHandler.invoke(LoanModalEvent.OnChangeDate)
        },
        onSetTime = {
            onEventHandler.invoke(LoanModalEvent.OnChangeTime)
        },
    )

    LoanRecordModal(
        modal = state.loanRecordModalData, onCreate = {
        onEventHandler.invoke(LoanRecordModalEvent.OnCreateLoanRecord(it))
    }, onEdit = {
        onEventHandler.invoke(LoanRecordModalEvent.OnEditLoanRecord(it))
    }, onDelete = { loanRecord ->
        onEventHandler.invoke(LoanRecordModalEvent.OnDeleteLoanRecord(loanRecord))
    }, accounts = state.accounts, dismiss = {
        onEventHandler.invoke(LoanRecordModalEvent.OnDismissLoanRecord)
    }, onCreateAccount = { createAccountData ->
        onEventHandler.invoke(LoanDetailsScreenEvent.OnCreateAccount(createAccountData))
    },
        dateTime = state.dateTime,
        onSetDate = {
            onEventHandler.invoke(LoanRecordModalEvent.OnChangeDate)
        },
        onSetTime = {
            onEventHandler.invoke(LoanRecordModalEvent.OnChangeTime)
        },
    )

    DeleteModal(
        visible = state.isDeleteModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.loan_confirm_deletion_description),
        dismiss = {
            onEventHandler.invoke(DeleteLoanModalEvent.OnDismissDeleteLoan(isDeleteModalVisible = false))
        }
    ) {
        onEventHandler.invoke(DeleteLoanModalEvent.OnDeleteLoan)
    }

    ProgressModal(
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_loan_change),
        visible = state.waitModalVisible
    )
}

@Preview
@Composable
private fun Preview_Empty() {
    IvyWalletPreview {
        UI(
            LoanDetailsScreenState(
                baseCurrency = "BGN",
                loan = previewLoan(
                    name = "Loan 1",
                    amount = 4023.54,
                    color = Red,
                    type = LoanType.LEND,
                    dateTime = LocalDateTime.now(),
                ),
                displayLoanRecords = persistentListOf(),
                progress = LoanProgress(total = 4023.54, paid = 3821.00, interestPaid = 100.0),
                accounts = persistentListOf(),
                selectedLoanAccount = null,
                createLoanTransaction = false,
                isDeleteModalVisible = false,
                loanModalData = null,
                loanRecordModalData = null,
                waitModalVisible = false,
                dateTime = Instant.now()
            )
        ) {}
    }
}

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35)

@Preview
@Composable
private fun Preview_Records(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(
            LoanDetailsScreenState(
                baseCurrency = "BGN",
                loan = previewLoan(
                    name = "Loan 1",
                    amount = 4023.54,
                    color = Red,
                    type = LoanType.LEND,
                    dateTime = testDateTime,
                ),
                displayLoanRecords = persistentListOf(
                    DisplayLoanRecord(
                        previewLoanRecord(
                            amount = 123.45,
                            dateTime = testDateTime.minusDays(1).toInstant(ZoneOffset.UTC),
                            type = LoanRecordType.INCREASE,
                            note = "Cash",
                        )
                    ),
                    DisplayLoanRecord(
                        previewLoanRecord(
                            amount = 0.50,
                            dateTime = testDateTime.minusYears(1).toInstant(ZoneOffset.UTC),
                            type = LoanRecordType.DECREASE,
                        )
                    ),
                    DisplayLoanRecord(
                        previewLoanRecord(
                            amount = 1000.00,
                            dateTime = testDateTime.minusMonths(1).toInstant(ZoneOffset.UTC),
                            type = LoanRecordType.INCREASE,
                            note = "Revolut",
                        )
                    ),
                ),
                progress = LoanProgress(total = 4023.54, paid = 3821.00, interestPaid = 100.0),
                accounts = persistentListOf(),
                selectedLoanAccount = null,
                createLoanTransaction = false,
                isDeleteModalVisible = false,
                loanModalData = null,
                loanRecordModalData = null,
                waitModalVisible = false,
                dateTime = Instant.now()
            )
        ) {}
    }
}
