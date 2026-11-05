package com.ivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.model.CreditCardId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.ChooseStartDateOfMonthModal
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalAddSave
import com.ivy.wallet.ui.theme.modal.ModalDelete
import com.ivy.wallet.ui.theme.modal.ModalNameInput
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.ui.theme.modal.dialogData
import com.ivy.wallet.utils.format
import java.util.UUID

/**
 * The credit card a [CreditCardModal] edits, of [account]: a new one when [card] is null. Its
 * limit is in the account's [currency].
 */
data class CreditCardModalData(
    val card: CreditCard?,
    val account: AccountId,
    val currency: String,
    val id: UUID = UUID.randomUUID(),
)

/** A credit card's name, statement closing day, charge day and limit; an existing one deletes too. */
@Composable
fun BoxWithConstraintsScope.CreditCardModal(
    modal: CreditCardModalData?,
    onSave: (CreditCard) -> Unit,
    onDelete: (CreditCard) -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    val card = shown?.card
    val currency = shown?.currency ?: ""
    var nameTextFieldValue by remember(shown) {
        mutableStateOf(selectEndTextFieldValue(card?.name?.value))
    }
    var chargeDay by remember(shown) { mutableStateOf(card?.chargeDay) }
    var closingDay by remember(shown) { mutableStateOf(card?.statementClosingDay) }
    var creditLimit by remember(shown) { mutableStateOf(card?.creditLimit?.value) }

    var chargeDayModalVisible by remember(shown) { mutableStateOf(false) }
    var closingDayModalVisible by remember(shown) { mutableStateOf(false) }
    var creditLimitModalVisible by remember(shown) { mutableStateOf(false) }

    IvyModal(
        modifier = modifier,
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        primaryAction = {
            val name = NotBlankTrimmedString.from(nameTextFieldValue.text).getOrNull()
            val day = chargeDay
            // A card needs its charge day to date its purchases.
            ModalAddSave(item = card, enabled = name != null && day != null) {
                if (shown == null || name == null || day == null) return@ModalAddSave
                onSave(
                    CreditCard(
                        id = card?.id ?: CreditCardId(UUID.randomUUID()),
                        account = shown.account,
                        name = name,
                        chargeDay = day,
                        statementClosingDay = closingDay,
                        creditLimit = creditLimit?.let(PositiveDouble::from)?.getOrNull(),
                    )
                )
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ModalTitle(
                text = stringResource(if (card != null) R.string.edit_credit_card else R.string.new_credit_card)
            )

            if (card != null) {
                Spacer(Modifier.weight(1f))

                ModalDelete(testTag = "credit_card_delete") {
                    onDelete(card)
                    dismiss()
                }

                Spacer(Modifier.width(24.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        ModalNameInput(
            hint = stringResource(R.string.credit_card_name),
            autoFocusKeyboard = card == null,
            textFieldValue = nameTextFieldValue,
            setTextFieldValue = { nameTextFieldValue = it },
        )

        Spacer(Modifier.height(24.dp))

        AccountSettingRow(
            label = stringResource(R.string.statement_closes),
            value = closingDay?.let { stringResource(R.string.day_of_month, it.toString()) }
                ?: stringResource(R.string.last_day_of_month),
            testTag = "credit_card_closing_day",
            onClick = { closingDayModalVisible = true },
        )

        Spacer(Modifier.height(12.dp))

        AccountSettingRow(
            label = stringResource(R.string.charge_day),
            value = chargeDay?.let { stringResource(R.string.day_of_month, it.toString()) }
                ?: stringResource(R.string.charge_day_not_set),
            testTag = "credit_card_charge_day",
            onClick = { chargeDayModalVisible = true },
        )

        Spacer(Modifier.height(12.dp))

        AccountSettingRow(
            label = stringResource(R.string.credit_limit),
            value = creditLimit?.let { "${it.format(currency)} $currency" }
                ?: stringResource(R.string.credit_limit_not_set),
            testTag = "credit_card_limit",
            onClick = { creditLimitModalVisible = true },
        )

        Spacer(Modifier.height(24.dp))
    }

    ChooseStartDateOfMonthModal(
        visible = closingDayModalVisible,
        selectedStartDateOfMonth = closingDay ?: LastDayOfMonth,
        title = stringResource(R.string.statement_closes),
        dismiss = { closingDayModalVisible = false },
    ) {
        // The 31st closes every month on its last day, which null already means.
        closingDay = it.takeIf { day -> day < LastDayOfMonth }
    }

    ChooseStartDateOfMonthModal(
        visible = chargeDayModalVisible,
        selectedStartDateOfMonth = chargeDay ?: 0,
        title = stringResource(R.string.charge_day),
        dismiss = { chargeDayModalVisible = false },
    ) {
        chargeDay = it
    }

    val creditLimitModalId = remember(shown, creditLimit) { UUID.randomUUID() }
    AmountModal(
        id = creditLimitModalId,
        visible = creditLimitModalVisible,
        currency = currency,
        initialAmount = creditLimit,
        dismiss = { creditLimitModalVisible = false }
    ) {
        creditLimit = it.takeIf { limit -> limit > 0 }
    }
}

private const val LastDayOfMonth = 31
