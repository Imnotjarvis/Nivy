package com.nivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.shapes.PillShape
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB1

/**
 * Two amounts side by side, each under its label, with a line between them: a month's income
 * and expenses, or what an investment cost and what it made.
 */
@Composable
fun TwoAmountsRow(
    startLabel: String,
    startAmount: Double,
    endLabel: String,
    endAmount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    startColor: Color = MaterialTheme.colorScheme.onSurface,
    endColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))

        LabelAmountColumn(
            label = startLabel,
            amount = startAmount,
            currency = currency,
            amountColor = startColor,
        )

        Spacer(Modifier.weight(1f))

        VerticalDivider(
            modifier = Modifier
                .height(48.dp)
                .clip(PillShape),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(Modifier.weight(1f))

        LabelAmountColumn(
            label = endLabel,
            amount = endAmount,
            currency = currency,
            amountColor = endColor,
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LabelAmountColumn(
    label: String,
    amount: Double,
    currency: String,
    amountColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmountCurrencyB1(
                textColor = amountColor,
                amount = amount,
                currency = currency
            )
        }
    }
}
