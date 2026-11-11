package com.nivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.NivyDividerLine
import androidx.compose.material3.MaterialTheme

@Composable
fun ModalAmountSection(
    label: String,
    currency: String,
    amount: Double,
    amountPaddingTop: Dp,
    amountPaddingBottom: Dp,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    showAmountModal: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NivyDividerLine()

        header?.invoke()

        Spacer(Modifier.height(amountPaddingTop))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(4.dp))

        BalanceRow(
            modifier = Modifier
                .clickableNoIndication(rememberInteractionSource()) {
                    showAmountModal()
                }
                .testTag("amount_balance"),
            currency = currency,
            balance = amount,

            balanceFontSize = 40.sp,

        )

        Spacer(Modifier.height(amountPaddingBottom))
    }
}
