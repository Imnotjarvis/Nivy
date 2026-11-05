package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BufferBattery
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import java.util.UUID

data class BufferModalData(
    val balance: Double,
    val buffer: Double,
    val currency: String,
    val id: UUID = UUID.randomUUID()
)

@Suppress("ParameterNaming")
@Composable
internal fun BoxWithConstraintsScope.BufferModal(
    modal: BufferModalData?,
    dismiss: () -> Unit,
    onBufferChanged: (Double) -> Unit
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    var newBufferAmount by remember(shown) {
        mutableDoubleStateOf(shown?.buffer ?: 0.0)
    }

    var amountModalVisible by remember { mutableStateOf(false) }

    IvyModal(
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        primaryAction = {
            ModalSave {
                onBufferChanged(newBufferAmount)
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(16.dp))

        BufferBattery(
            modifier = Modifier.padding(horizontal = 16.dp),
            buffer = newBufferAmount,
            balance = shown?.balance ?: 0.0,
            currency = shown?.currency ?: "",
            backgroundNotFilled = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        ModalAmountSection(
            label = stringResource(R.string.edit_savings_goal),
            currency = shown?.currency ?: "",
            amount = newBufferAmount
        ) {
            amountModalVisible = true
        }
    }

    val amountModalId = remember(shown, newBufferAmount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = shown?.currency ?: "",
        initialAmount = newBufferAmount,
        dismiss = { amountModalVisible = false }
    ) {
        newBufferAmount = it
    }
}
