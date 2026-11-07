package com.nivy.wallet.ui.edit.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CopyAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import androidx.compose.ui.graphics.SolidColor
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.CloseButton
import com.nivy.wallet.ui.theme.components.DeleteButton
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import java.util.UUID
import androidx.compose.material3.MaterialTheme

@Composable
fun Toolbar(
    type: TransactionType,
    initialTransactionId: UUID?,

    onDeleteTrnModal: () -> Unit,
    onChangeTransactionTypeModal: () -> Unit,

    showDuplicateButton: Boolean,
    modifier: Modifier = Modifier,
    onDuplicate: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val nav = navigation()
        CloseButton {
            nav.back()
        }

        Spacer(Modifier.weight(1f))

        when (type) {
            TransactionType.INCOME -> {
                NivyOutlinedButton(
                    text = stringResource(R.string.income),
                    iconStart = R.drawable.ic_income
                ) {
                    onChangeTransactionTypeModal()
                }

                Spacer(Modifier.width(12.dp))
            }

            TransactionType.EXPENSE -> {
                NivyOutlinedButton(
                    text = stringResource(R.string.expense),
                    iconStart = R.drawable.ic_expense
                ) {
                    onChangeTransactionTypeModal()
                }

                Spacer(Modifier.width(12.dp))
            }

            else -> {
                // show nothing
            }
        }

        if (initialTransactionId != null) {
            if (showDuplicateButton) {
                OutlinedIconButton(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Transparent, PillShape)
                        .testTag("duplicate_button"),
                    shape = PillShape,
                    colors = IconButtonDefaults.outlinedIconButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = IconButtonDefaults.outlinedIconButtonBorder(enabled = true)
                        .copy(width = 2.dp, brush = SolidColor(MaterialTheme.colorScheme.surfaceVariant)),
                    onClick = onDuplicate
                ) {
                    Icon(
                        modifier = Modifier.padding(6.dp),
                        imageVector = Icons.Sharp.CopyAll,
                        contentDescription = stringResource(R.string.duplicate),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(12.dp))
            }

            DeleteButton {
                onDeleteTrnModal()
            }

            Spacer(Modifier.width(24.dp))
        }
    }
}
