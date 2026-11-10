package com.nivy.loans.loandetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.model.LoanRecordType
import com.nivy.data.model.Account
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.design.utils.thenIf
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.theme.components.AccountsRow
import com.nivy.wallet.ui.theme.components.DateTimeRow
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateLoanRecordData
import com.nivy.wallet.domain.logic.model.EditLoanRecordData
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyCheckboxWithText
import com.nivy.wallet.ui.theme.modal.DeleteModal
import com.nivy.wallet.ui.theme.modal.ModalAddSave
import com.nivy.wallet.ui.theme.modal.ModalAmountSection
import com.nivy.wallet.ui.theme.modal.ModalDelete
import com.nivy.wallet.ui.theme.modal.ModalNameInput
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.dialogData
import com.nivy.wallet.ui.theme.modal.edit.AccountModal
import com.nivy.wallet.ui.theme.modal.edit.AccountModalData
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import java.time.Instant
import java.util.UUID
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme

data class LoanRecordModalData(
    val loanRecord: LoanRecord?,
    val baseCurrency: String,
    val loanAccountCurrencyCode: String? = null,
    val selectedAccount: Account? = null,
    val createLoanRecordTransaction: Boolean = false,
    val isLoanInterest: Boolean = false,
    val id: UUID = UUID.randomUUID(),
)

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun BoxWithConstraintsScope.LoanRecordModal(
    modal: LoanRecordModalData?,
    dateTime: Instant,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onCreate: (CreateLoanRecordData) -> Unit,
    onEdit: (EditLoanRecordData) -> Unit,
    onDelete: (LoanRecord) -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData) -> Unit = {},
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    val initialRecord = shown?.loanRecord

    var noteTextFieldValue by remember(shown?.id) {
        mutableStateOf(selectEndTextFieldValue(initialRecord?.note?.value))
    }
    var currencyCode by remember(shown?.id) {
        mutableStateOf(shown?.baseCurrency ?: "")
    }
    var amount by remember(shown?.id) {
        mutableDoubleStateOf(shown?.loanRecord?.amount?.value ?: 0.0)
    }
    var selectedAcc by remember(shown?.id) {
        mutableStateOf(shown?.selectedAccount)
    }
    var createLoanRecordTrans by remember(shown?.id) {
        mutableStateOf(shown?.createLoanRecordTransaction ?: false)
    }
    var loanInterest by remember(shown?.id) {
        mutableStateOf(shown?.isLoanInterest ?: false)
    }
    var reCalculate by remember(shown?.id) {
        mutableStateOf(false)
    }
    var reCalculateVisible by remember(shown?.id) {
        mutableStateOf(shown?.loanAccountCurrencyCode != null && shown.loanAccountCurrencyCode != shown.baseCurrency)
    }
    var loanRecordType by remember(shown?.id) {
        mutableStateOf(shown?.loanRecord?.type ?: LoanRecordType.DECREASE)
    }

    var dateTime = shown?.loanRecord?.dateTime ?: dateTime
    var amountModalVisible by remember { mutableStateOf(false) }
    var deleteModalVisible by remember(shown?.id) { mutableStateOf(false) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var accountChangeConformationModal by remember { mutableStateOf(false) }

    NivyModal(
        modifier = modifier,
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        shiftIfKeyboardShown = true,
        primaryAction = {
            ModalAddSave(
                item = initialRecord,
                enabled = amount > 0 && selectedAcc != null
            ) {
                accountChangeConformationModal =
                    initialRecord != null && shown.selectedAccount != null &&
                            shown.baseCurrency != currencyCode && currencyCode != shown.loanAccountCurrencyCode

                if (!accountChangeConformationModal) {
                    save(
                        loanRecord = initialRecord,
                        noteTextFieldValue = noteTextFieldValue,
                        amount = amount,
                        dateTime = dateTime,
                        loanRecordInterest = loanInterest,
                        selectedAccount = selectedAcc,
                        createLoanRecordTransaction = createLoanRecordTrans,
                        reCalculateAmount = reCalculate,
                        loanRecordType = loanRecordType,

                        onCreate = onCreate,
                        onEdit = onEdit,
                        dismiss = dismiss,
                    )
                }
            }
        }
    ) {
        LaunchedEffect(Unit) {
            if (shown?.loanRecord == null) {
                amountModalVisible = true
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalTitle(
                text = if (initialRecord != null) {
                    stringResource(R.string.edit_record)
                } else {
                    stringResource(
                        R.string.new_record
                    )
                }
            )

            if (initialRecord != null) {
                Spacer(Modifier.weight(1f))

                ModalDelete {
                    deleteModalVisible = true
                }

                Spacer(Modifier.width(24.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        ModalNameInput(
            hint = stringResource(R.string.note),
            autoFocusKeyboard = false,
            textFieldValue = noteTextFieldValue,
            setTextFieldValue = {
                noteTextFieldValue = it
            }
        )

        Spacer(Modifier.height(24.dp))

        val timeConverter = LocalTimeConverter.current
        DateTimeRow(
            dateTime = with(timeConverter) { dateTime.toLocalDateTime() },
            onEditDate = onSetDate,
            onEditTime = onSetTime,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.associated_account),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(16.dp))

        AccountsRow(
            accounts = accounts,
            selectedAccount = selectedAcc,
            onSelectedAccountChanged = {
                currencyCode = it.asset.code

                reCalculateVisible =
                    initialRecord?.convertedAmount != null && selectedAcc != null && currencyCode == shown.baseCurrency
                // Unchecks the Recalculate Option if Recalculate Checkbox is not visible
                reCalculate = !reCalculateVisible

                selectedAcc = it
            },
            onAddNewAccount = {
                accountModalData = AccountModalData(
                    account = null,
                    baseCurrency = selectedAcc?.asset?.code ?: currencyCode,
                    balance = 0.0
                )
            },
            childrenTestTag = "amount_modal_account"
        )
        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.loan_record_type),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(16.dp))

        LoanRecordTypeRow(selectedRecordType = loanRecordType, onLoanRecordTypeChanged = {
            if (it == LoanRecordType.INCREASE) loanInterest = false
            loanRecordType = it
        })

        Spacer(Modifier.height(16.dp))

        NivyCheckboxWithText(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.Start),
            text = stringResource(R.string.create_main_transaction),
            checked = createLoanRecordTrans
        ) {
            createLoanRecordTrans = it
        }

        AnimatedVisibility(visible = loanRecordType == LoanRecordType.DECREASE) {
            NivyCheckboxWithText(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.Start),
                text = stringResource(R.string.mark_as_interest),
                checked = loanInterest
            ) {
                loanInterest = it
            }
        }

        if (reCalculateVisible) {
            NivyCheckboxWithText(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .align(Alignment.Start),
                text = stringResource(R.string.recalculate_amount_with_today_exchange_rates),
                checked = reCalculate
            ) {
                reCalculate = it
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ModalAmountSection(
            label = stringResource(R.string.enter_record_amount_uppercase),
            currency = currencyCode,
            amount = amount,
            amountPaddingTop = 40.dp,
            amountPaddingBottom = 40.dp,
        ) {
            amountModalVisible = true
        }
    }

    val amountModalId = remember(shown, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currencyCode,
        initialAmount = amount,
        dismiss = { amountModalVisible = false }
    ) { newAmount ->
        amount = newAmount
    }

    DeleteModal(
        visible = deleteModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.record_deletion_warning, noteTextFieldValue.text),
        dismiss = { deleteModalVisible = false }
    ) {
        if (initialRecord != null) {
            onDelete(initialRecord)
        }
        deleteModalVisible = false
        reCalculate = false
        dismiss()
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )

    DeleteModal(
        visible = accountChangeConformationModal,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.account_change_warning),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = {
            selectedAcc = shown?.selectedAccount ?: selectedAcc
            accountChangeConformationModal = false
        }
    ) {
        save(
            loanRecord = initialRecord,
            noteTextFieldValue = noteTextFieldValue,
            amount = amount,
            dateTime = dateTime,
            loanRecordInterest = loanInterest,
            selectedAccount = selectedAcc,
            createLoanRecordTransaction = createLoanRecordTrans,
            reCalculateAmount = reCalculate,
            loanRecordType = loanRecordType,

            onCreate = onCreate,
            onEdit = onEdit,
            dismiss = dismiss,
        )

        accountChangeConformationModal = false
    }
}

private fun save(
    loanRecord: LoanRecord?,
    noteTextFieldValue: TextFieldValue,
    amount: Double,
    dateTime: Instant,
    loanRecordInterest: Boolean = false,
    createLoanRecordTransaction: Boolean = false,
    selectedAccount: Account? = null,
    reCalculateAmount: Boolean = false,
    loanRecordType: LoanRecordType,

    onCreate: (CreateLoanRecordData) -> Unit,
    onEdit: (EditLoanRecordData) -> Unit,
    dismiss: () -> Unit
) {
    if (loanRecord != null) {
        val positiveAmount = PositiveDouble.from(amount).getOrNull()
        if (positiveAmount != null) {
            val record = loanRecord.copy(
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull(),
                amount = positiveAmount,
                dateTime = dateTime,
                interest = loanRecordInterest,
                account = selectedAccount?.id,
                type = loanRecordType
            )
            onEdit(
                EditLoanRecordData(
                    newLoanRecord = record,
                    originalLoanRecord = loanRecord,
                    createLoanRecordTransaction = createLoanRecordTransaction,
                    reCalculateLoanAmount = reCalculateAmount,
                )
            )
        }
    } else {
        onCreate(
            CreateLoanRecordData(
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value,
                amount = amount,
                dateTime = dateTime,
                interest = loanRecordInterest,
                account = selectedAccount,
                createLoanRecordTransaction = createLoanRecordTransaction,
                loanRecordType = loanRecordType
            )
        )
    }

    dismiss()
}

@Composable
@Suppress("ParameterNaming")
private fun LoanRecordTypeRow(
    selectedRecordType: LoanRecordType?,
    modifier: Modifier = Modifier,
    onLoanRecordTypeChanged: (LoanRecordType) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(24.dp))
        LoanRecordType(
            modifier = Modifier,
            loanRecordType = LoanRecordType.DECREASE,
            selectedRecordType = selectedRecordType
        ) {
            onLoanRecordTypeChanged(it)
        }
        Spacer(modifier = Modifier.width(8.dp))
        LoanRecordType(
            modifier = Modifier,
            loanRecordType = LoanRecordType.INCREASE,
            selectedRecordType = selectedRecordType
        ) {
            onLoanRecordTypeChanged(it)
        }
    }
}

@Composable
private fun LoanRecordType(
    loanRecordType: LoanRecordType,
    selectedRecordType: LoanRecordType?,
    modifier: Modifier = Modifier,
    onClick: (LoanRecordType) -> Unit
) {
    val (text, iconDrawable) =
        if (loanRecordType == LoanRecordType.INCREASE) {
            stringResource(id = R.string.increase_loan) to R.drawable.ic_donate_plus
        } else {
            stringResource(id = R.string.decrease_loan) to R.drawable.ic_donate_minus
        }
    val selected = selectedRecordType == loanRecordType
    val medium = MaterialTheme.colorScheme.surfaceVariant
    val rFull = PillShape
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer
    Row(
        modifier = modifier
            .clip(PillShape)
            .thenIf(!selected) {
                border(2.dp, medium, rFull)
            }
            .thenIf(selected) {
                background(selectedColor, rFull)
            }
            .clickable(onClick = { onClick(loanRecordType) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        ItemIconSDefaultIcon(
            defaultIcon = iconDrawable,
            iconName = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        LoanRecordModal(
            modal = LoanRecordModalData(
                loanRecord = null,
                baseCurrency = "BGN"
            ),
            onCreate = {},
            onEdit = {},
            onDelete = {},
            dismiss = {},
            onSetDate = {},
            onSetTime = {},
            dateTime = Instant.now()
        )
    }
}
