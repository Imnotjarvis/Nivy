package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.data.model.AccountId
import com.nivy.data.model.AccountValue
import com.nivy.data.model.AccountValueId
import com.nivy.data.model.primitive.NonNegativeDouble
import com.nivy.design.system.colors.Gray
import com.nivy.domain.RootScreen
import com.nivy.ui.R
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalAddSave
import com.nivy.wallet.ui.theme.modal.ModalDelete
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.dialogData
import com.nivy.wallet.utils.format
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import java.time.LocalTime
import java.util.UUID

/**
 * The value an [AccountValueModal] edits, of [account] in [currency]: a new one when [value] is
 * null.
 */
data class AccountValueModalData(
    val value: AccountValue?,
    val account: AccountId,
    val currency: String,
    val id: UUID = UUID.randomUUID(),
)

/**
 * What an investment account is worth on a day: its investments, and the cash in it that isn't
 * invested, if any. One entered before deletes too.
 */
@Composable
fun BoxWithConstraintsScope.AccountValueModal(
    modal: AccountValueModalData?,
    onSave: (AccountValue) -> Unit,
    onDelete: (AccountValue) -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    val existing = shown?.value
    val currency = shown?.currency ?: ""
    val timeProvider = LocalTimeProvider.current
    val timeConverter = LocalTimeConverter.current
    val today = timeProvider.localDateNow()
    val enteredOn = existing?.time?.let { with(timeConverter) { it.toLocalDate() } }

    var amount by remember(shown) { mutableStateOf(existing?.value?.value) }
    var cash by remember(shown) { mutableStateOf(existing?.cash?.value) }
    var cashModalVisible by remember(shown) { mutableStateOf(false) }
    var date by remember(shown) { mutableStateOf(enteredOn ?: today) }
    // A new value starts on the keypad, like a new transaction.
    var amountModalVisible by remember(shown) { mutableStateOf(shown != null && existing == null) }

    NivyModal(
        modifier = modifier,
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        primaryAction = {
            val value = amount
            ModalAddSave(item = existing, enabled = value != null) {
                if (shown == null || value == null) return@ModalAddSave
                val time = when {
                    // Kept as it was: what moved in or out since stays after it.
                    existing != null && date == enteredOn -> existing.time
                    // Today's, entered now: what moves in or out later today comes after it.
                    date == today -> timeProvider.utcNow()
                    // A past day's, at its end: that day's transfers are in it.
                    else -> with(timeConverter) { date.atTime(LocalTime.MAX).toUTC() }
                }
                onSave(
                    AccountValue(
                        id = existing?.id ?: AccountValueId(UUID.randomUUID()),
                        account = shown.account,
                        time = time,
                        value = NonNegativeDouble.unsafe(value),
                        cash = cash?.let(NonNegativeDouble::unsafe),
                    )
                )
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ModalTitle(
                text = stringResource(if (existing != null) R.string.edit_value else R.string.update_value)
            )

            if (existing != null) {
                Spacer(Modifier.weight(1f))

                ModalDelete(testTag = "account_value_delete") {
                    onDelete(existing)
                    dismiss()
                }

                Spacer(Modifier.width(24.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        AccountSettingRow(
            label = stringResource(R.string.investments),
            value = amount?.let { "${it.format(currency)} $currency" } ?: "",
            testTag = "account_value_amount",
            onClick = { amountModalVisible = true },
        )

        Spacer(Modifier.height(12.dp))

        AccountSettingRow(
            label = stringResource(R.string.cash),
            value = cash?.let { "${it.format(currency)} $currency" } ?: stringResource(R.string.cash_not_set),
            testTag = "account_value_cash",
            onClick = { cashModalVisible = true },
        )

        Spacer(Modifier.height(12.dp))

        val context = LocalContext.current
        val timeFormatter = LocalTimeFormatter.current
        AccountSettingRow(
            label = stringResource(R.string.value_date),
            value = with(timeFormatter) {
                date.atStartOfDay().format(TimeFormatter.Style.DateOnly(includeWeekDay = true))
            },
            testTag = "account_value_date",
            // A value can't be from the future.
            onClick = { (context as RootScreen).pickDate(initialDate = date, maxDate = today) { date = it } },
        )

        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.investments_and_cash_hint),
            style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.SemiBold)
        )

        Spacer(Modifier.height(24.dp))
    }

    val amountModalId = remember(shown) { UUID.randomUUID() }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currency,
        initialAmount = amount,
        dismiss = { amountModalVisible = false }
    ) {
        amount = it
    }

    val cashModalId = remember(shown) { UUID.randomUUID() }
    AmountModal(
        id = cashModalId,
        visible = cashModalVisible,
        currency = currency,
        initialAmount = cash,
        dismiss = { cashModalVisible = false }
    ) {
        // Zero is kept: none is uninvested now, and money moved in later waits as cash.
        cash = it
    }
}
