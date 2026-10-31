package com.ivy.wallet.ui.theme.wallet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.shortenAmount
import com.ivy.wallet.utils.shouldShortAmount
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.design.system.typography.numberTitleLarge

@SuppressLint("ComposeModifierMissing")
@Composable
fun AmountCurrencyB2Row(
    amount: Double,
    currency: String,
    amountFontWeight: FontWeight = FontWeight.ExtraBold,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = amount.format(currency),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = textColor,
                fontWeight = amountFontWeight
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = currency,
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun AmountCurrencyB1Row(
    amount: Double,
    currency: String,
    amountFontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AmountCurrencyB1(
            amount = amount,
            currency = currency,
            amountFontWeight = amountFontWeight,
            textColor = textColor
        )
    }
}

@SuppressLint(
    "ComposeContentEmitterReturningValues",
    "ComposeMultipleContentEmitters",
    "ComposeModifierMissing"
)
@Composable
fun AmountCurrencyB1(
    amount: Double,
    currency: String,
    amountFontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    shortenBigNumbers: Boolean = false
) {
    val shortAmount = shortenBigNumbers && shouldShortAmount(amount)
    val text = if (shortAmount) shortenAmount(amount) else amount.format(currency)
    Text(
        modifier = Modifier.testTag("amount_currency_b1"),
        text = text,
        style = MaterialTheme.typography.numberTitleLarge.copy(
            color = textColor,
            fontWeight = amountFontWeight
        )
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = currency,
        style = MaterialTheme.typography.numberTitleLarge.copy(
            color = textColor,
            fontWeight = FontWeight.Normal
        )
    )
}
