package com.nivy.wallet.ui.theme.wallet

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
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.shortenAmount
import com.nivy.wallet.utils.shouldShortAmount
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.typography.numberTitleLarge

@Composable
fun AmountCurrencyB2Row(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    amountFontWeight: FontWeight = FontWeight.ExtraBold,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
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

@Composable
fun AmountCurrencyB1Row(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    amountFontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
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

@Composable
fun AmountCurrencyB1(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    amountFontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    shortenBigNumbers: Boolean = false,
    // A sign before the amount, like a transaction's "+" or "-".
    prefix: String = "",
) {
    val shortAmount = shortenBigNumbers && shouldShortAmount(amount)
    val text = prefix + if (shortAmount) shortenAmount(amount) else amount.format(currency)
    Text(
        modifier = modifier.testTag("amount_currency_b1"),
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
