package com.ivy.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import com.ivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.clip

@Composable
fun IncomeExpensesRow(
    income: Double,
    expenses: Double,
    currency: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    incomeColor: Color = textColor,
    expensesColor: Color = textColor,
    dividerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    incomeLabel: String = stringResource(R.string.income_uppercase),
    expensesLabel: String = stringResource(R.string.expenses_uppercase),
    center: Boolean = true,
    dividerSpacer: Dp? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (center) {
            Spacer(Modifier.weight(1f))
        }

        LabelAmountColumn(
            textColor = textColor,
            amountColor = incomeColor,
            label = incomeLabel,
            amount = income,
            currency = currency,
            center = center
        )

        if (center) {
            Spacer(Modifier.weight(1f))
        }

        if (dividerSpacer != null) {
            Spacer(modifier = Modifier.width(dividerSpacer))
        }

        VerticalDivider(
            modifier = Modifier
                .height(48.dp)
                .clip(PillShape),
            thickness = 2.dp,
            color = dividerColor
        )

        if (center) {
            Spacer(Modifier.weight(1f))
        }

        if (dividerSpacer != null) {
            Spacer(modifier = Modifier.width(dividerSpacer))
        }

        LabelAmountColumn(
            textColor = textColor,
            amountColor = expensesColor,
            label = expensesLabel,
            amount = expenses,
            currency = currency,
            center = center
        )

        if (center) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LabelAmountColumn(
    label: String,
    amount: Double,
    currency: String,
    textColor: Color,
    amountColor: Color,
    center: Boolean
) {
    Column(
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
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
