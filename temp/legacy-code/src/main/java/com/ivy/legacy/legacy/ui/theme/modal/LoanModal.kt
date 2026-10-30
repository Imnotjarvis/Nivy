package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Account
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.domain.legacy.ui.IvyColorPicker
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.legacy.ui.theme.components.DateTimeRow
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.design.utils.thenIf
import com.ivy.legacy.legacy.ui.theme.modal.ModalNameInput
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.design.system.colors.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.modal.edit.IconNameRow
import com.ivy.design.system.colors.toComposeColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

data class LoanModalData(
    val loan: Loan?,
    val baseCurrency: String,
    val selectedAccount: Account? = null,
    val autoFocusKeyboard: Boolean = true,
    val autoOpenAmountModal: Boolean = false,
    val createLoanTransaction: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun BoxWithConstraintsScope.LoanModal(
    modal: LoanModalData?,
    dateTime: Instant,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onCreateLoan: (CreateLoanData) -> Unit,
    onEditLoan: (Loan, Boolean) -> Unit,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData) -> Unit = {},
    onPerformCalculations: () -> Unit = {},
    dismiss: () -> Unit,
) {
    val loan = modal?.loan
    val timeConverter = LocalTimeConverter.current

    var nameTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(loan?.name?.value))
    }
    var dateTime = modal?.loan?.dateTime ?: with(timeConverter) {
        dateTime.toLocalDateTime()
    }
    var type by remember(modal) {
        mutableStateOf(modal?.loan?.type ?: LoanType.BORROW)
    }
    var amount by remember(modal) {
        mutableStateOf(modal?.loan?.amount?.value ?: 0.0)
    }
    var color by remember(modal) {
        mutableStateOf(loan?.color?.let { Color(it.value) } ?: Ivy)
    }
    var icon by remember(modal) {
        mutableStateOf(loan?.icon?.id)
    }
    var noteTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(loan?.note?.value))
    }
    var currencyCode by remember(modal) {
        mutableStateOf(modal?.baseCurrency ?: "")
    }

    var selectedAcc by remember(modal) {
        mutableStateOf(modal?.selectedAccount)
    }

    var createLoanTrans by remember(modal) {
        mutableStateOf(modal?.createLoanTransaction ?: false)
    }

    var accountChangeModal by remember { mutableStateOf(false) }
    var amountModalVisible by remember { mutableStateOf(false) }
    var currencyModalVisible by remember { mutableStateOf(false) }
    var chooseIconModalVisible by remember(modal) {
        mutableStateOf(false)
    }

    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        dismiss = dismiss,
        shiftIfKeyboardShown = false,
        PrimaryAction = {
            ModalAddSave(
                item = modal?.loan,
                // enabled = nameTextFieldValue.text.isNotNullOrBlank() && amount > 0 && ((createLoanTrans && selectedAcc != null) || !createLoanTrans)
                enabled = nameTextFieldValue.text.isNotNullOrBlank() && amount > 0 && selectedAcc != null
            ) {
                accountChangeModal =
                    loan != null && modal.selectedAccount != null &&
                    currencyCode != modal.selectedAccount.asset.code

                if (!accountChangeModal) {
                    save(
                        loan = loan,
                        nameTextFieldValue = nameTextFieldValue,
                        dateTime = dateTime,
                        noteTextFieldValue = noteTextFieldValue,
                        type = type,
                        color = color,
                        icon = icon,
                        amount = amount,
                        selectedAccount = selectedAcc,
                        createLoanTransaction = createLoanTrans,

                        onCreateLoan = onCreateLoan,
                        onEditLoan = onEditLoan,
                        dismiss = dismiss
                    )
                }
            }
        }
    ) {
        LaunchedEffect(Unit) {
            if (modal?.autoOpenAmountModal == true) {
                amountModalVisible = true
            }
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = if (modal?.loan != null) stringResource(R.string.edit_loan) else stringResource(R.string.new_loan),
        )

        Spacer(Modifier.height(24.dp))

        IconNameRow(
            hint = stringResource(R.string.loan_name),
            defaultIcon = R.drawable.ic_custom_loan_m,
            color = color,
            icon = icon,

            autoFocusKeyboard = modal?.autoFocusKeyboard ?: true,

            nameTextFieldValue = nameTextFieldValue,
            setNameTextFieldValue = { nameTextFieldValue = it },
            showChooseIconModal = {
                chooseIconModalVisible = true
            }
        )

        Spacer(Modifier.height(24.dp))

        DateTimeRow(
            dateTime = dateTime,
            onEditDate = onSetDate,
            onEditTime = onSetTime
        )

        Spacer(Modifier.height(24.dp))

        LoanTypePicker(
            type = type,
            onTypeSelected = { type = it }
        )

        Spacer(Modifier.height(24.dp))

        IvyColorPicker(
            selectedColor = color,
            onColorSelected = { color = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.note),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        ModalNameInput(
            hint = stringResource(R.string.description_text_field_hint),
            autoFocusKeyboard = false,
            textFieldValue = noteTextFieldValue,
            setTextFieldValue = {
                noteTextFieldValue = it
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
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
                selectedAcc = it
                currencyCode = it.asset.code
            },
            onAddNewAccount = {
                accountModalData = AccountModalData(
                    account = null,
                    baseCurrency = selectedAcc?.asset?.code ?: "USD",
                    balance = 0.0
                )
            },
            childrenTestTag = "amount_modal_account"
        )

        Spacer(Modifier.height(16.dp))

        IvyCheckboxWithText(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.Start),
            text = stringResource(R.string.create_main_transaction),
            checked = createLoanTrans
        ) {
            createLoanTrans = it
        }

        Spacer(modifier = Modifier.height(24.dp))

        ModalAmountSection(
            label = stringResource(R.string.enter_loan_amount_uppercase),
            currency = currencyCode,
            amount = amount,
            amountPaddingTop = 40.dp,
            amountPaddingBottom = 40.dp,
        ) {
            amountModalVisible = true
        }
    }

    val amountModalId = remember(modal, amount) {
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

    CurrencyModal(
        title = stringResource(R.string.choose_currency),
        initialCurrency = IvyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        currencyCode = it
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon ?: "loan",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it
    }

    DeleteModal(
        visible = accountChangeModal,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_warning),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = {
            selectedAcc = modal?.selectedAccount ?: selectedAcc
            accountChangeModal = false
        }
    ) {
        onPerformCalculations()
        save(
            loan = loan,
            nameTextFieldValue = nameTextFieldValue,
            dateTime = dateTime,
            noteTextFieldValue = noteTextFieldValue,
            type = type,
            color = color,
            icon = icon,
            amount = amount,
            selectedAccount = selectedAcc,
            createLoanTransaction = createLoanTrans,

            onCreateLoan = onCreateLoan,
            onEditLoan = onEditLoan,
            dismiss = dismiss
        )
        accountChangeModal = false
    }
}

@Composable
@Suppress("ParameterNaming")
private fun AccountsRow(
    accounts: List<Account>,
    selectedAccount: Account?,
    modifier: Modifier = Modifier,
    childrenTestTag: String? = null,
    onSelectedAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit
) {
    val lazyState = rememberLazyListState()

    LaunchedEffect(accounts, selectedAccount) {
        if (selectedAccount != null) {
            val selectedIndex = accounts.indexOf(selectedAccount)
            if (selectedIndex != -1) {
                launch {
                    lazyState.scrollToItem(
                        index = selectedIndex, // +1 because Spacer width 24.dp
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        state = lazyState
    ) {
        item {
            Spacer(Modifier.width(24.dp))
        }

        itemsIndexed(accounts) { _, account ->
            Account(
                account = account,
                selected = selectedAccount == account,
                testTag = childrenTestTag ?: "account"
            ) {
                onSelectedAccountChanged(account)
            }
        }

        item {
            AddAccount {
                onAddNewAccount()
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun Account(
    account: Account,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val accountColor = account.color.value.toComposeColor()
    val textColor =
        if (selected) findContrastTextColor(accountColor) else MaterialTheme.colorScheme.onSurface

    val medium = MaterialTheme.colorScheme.surfaceVariant
    val rFull = CircleShape

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .thenIf(!selected) {
                border(2.dp, medium, rFull)
            }
            .thenIf(selected) {
                background(accountColor, rFull)
            }
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        ItemIconSDefaultIcon(
            iconName = account.icon?.id,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = textColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = account.name.value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }

    Spacer(Modifier.width(8.dp))
}

@Composable
private fun AddAccount(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(
            icon = R.drawable.ic_plus,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = stringResource(R.string.add_account),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }

    Spacer(Modifier.width(8.dp))
}

@Suppress("ParameterNaming")
@Composable
private fun ColumnScope.LoanTypePicker(
    type: LoanType,
    onTypeSelected: (LoanType) -> Unit
) {
    Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = stringResource(R.string.loan_type),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        SelectorButton(
            selected = type == LoanType.BORROW,
            label = stringResource(R.string.borrow_money)
        ) {
            onTypeSelected(LoanType.BORROW)
        }

        Spacer(Modifier.width(8.dp))

        SelectorButton(
            selected = type == LoanType.LEND,
            label = stringResource(R.string.lend_money)
        ) {
            onTypeSelected(LoanType.LEND)
        }

        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun RowScope.SelectorButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val rFull = CircleShape
    Text(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable {
                onClick()
            }
            .padding(vertical = 8.dp)
            .thenIf(selected) {
                background(GradientIvy.asHorizontalBrush(), rFull)
            }
            .padding(vertical = 8.dp),
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = if (selected) White else Gray,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )
}

private fun save(
    loan: Loan?,
    nameTextFieldValue: TextFieldValue,
    dateTime: LocalDateTime,
    noteTextFieldValue: TextFieldValue,
    type: LoanType,
    color: Color,
    icon: String?,
    amount: Double,
    selectedAccount: Account? = null,
    createLoanTransaction: Boolean = false,

    onCreateLoan: (CreateLoanData) -> Unit,
    onEditLoan: (Loan, Boolean) -> Unit,
    dismiss: () -> Unit
) {
    if (loan != null) {
        val name = NotBlankTrimmedString.from(nameTextFieldValue.text).getOrNull()
        val positiveAmount = PositiveDouble.from(amount).getOrNull()
        if (name != null && positiveAmount != null) {
            onEditLoan(
                loan.copy(
                    name = name,
                    dateTime = dateTime,
                    note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull(),
                    type = type,
                    amount = positiveAmount,
                    color = ColorInt(color.toArgb()),
                    icon = icon?.let(IconAsset::from)?.getOrNull(),
                    account = selectedAccount?.id
                ),
                createLoanTransaction
            )
        }
    } else {
        onCreateLoan(
            CreateLoanData(
                name = nameTextFieldValue.text.trim(),
                type = type,
                amount = amount,
                color = color,
                icon = icon,
                account = selectedAccount,
                createLoanTransaction = createLoanTransaction,
                dateTime = dateTime,
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value
            )
        )
    }

    dismiss()
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        LoanModal(
            modal = LoanModalData(
                loan = null,
                baseCurrency = "BGN",
            ),
            onCreateLoan = { },
            onEditLoan = { _, _ -> },
            onSetTime = { },
            onSetDate = { },
            dateTime = Instant.now()
        ) {
        }
    }
}
