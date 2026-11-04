package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.wallet.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.ModalTitle
import java.util.UUID
import androidx.compose.material3.MaterialTheme

@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.ChangeTransactionTypeModal(
    visible: Boolean,
    includeTransferType: Boolean,
    initialType: TransactionType,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.set_transaction_type),
    id: UUID = UUID.randomUUID(),
    onTransactionTypeChanged: (TransactionType) -> Unit
) {
    var transactionType by remember(id) {
        mutableStateOf(initialType)
    }

    IvyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalSet {
                save(
                    transactionType = transactionType,
                    onTransactionTypeChanged = onTransactionTypeChanged,
                    dismiss = dismiss,
                )
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(32.dp))

        SelectableOptionRow(
            modifier = Modifier.testTag("modal_type_INCOME"),
            text = stringResource(R.string.income),
            icon = R.drawable.ic_income,
            selected = transactionType == TransactionType.INCOME,
            selectedFill = Green,
            selectedContent = White,
        ) {
            transactionType = TransactionType.INCOME
            save(
                transactionType = transactionType,
                onTransactionTypeChanged = onTransactionTypeChanged,
                dismiss = dismiss,
            )
        }

        Spacer(Modifier.height(12.dp))

        SelectableOptionRow(
            modifier = Modifier.testTag("modal_type_EXPENSE"),
            text = stringResource(R.string.expense),
            icon = R.drawable.ic_expense,
            selected = transactionType == TransactionType.EXPENSE,
            selectedFill = MaterialTheme.colorScheme.onSurface,
            selectedContent = MaterialTheme.colorScheme.surface,
        ) {
            transactionType = TransactionType.EXPENSE
            save(
                transactionType = transactionType,
                onTransactionTypeChanged = onTransactionTypeChanged,
                dismiss = dismiss,
            )
        }

        if (includeTransferType) {
            Spacer(Modifier.height(12.dp))

            SelectableOptionRow(
                modifier = Modifier.testTag("modal_type_TRANSFER"),
                text = stringResource(R.string.transfer),
                icon = R.drawable.ic_transfer,
                selected = transactionType == TransactionType.TRANSFER,
                selectedFill = Ivy,
                selectedContent = White,
            ) {
                transactionType = TransactionType.TRANSFER
                save(
                    transactionType = transactionType,
                    onTransactionTypeChanged = onTransactionTypeChanged,
                    dismiss = dismiss,
                )
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

private fun save(
    transactionType: TransactionType,
    onTransactionTypeChanged: (TransactionType) -> Unit,
    dismiss: () -> Unit
) {
    onTransactionTypeChanged(transactionType)
    dismiss()
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        ChangeTransactionTypeModal(
            includeTransferType = true,
            visible = true,
            initialType = TransactionType.INCOME,
            dismiss = {}
        ) {
        }
    }
}
