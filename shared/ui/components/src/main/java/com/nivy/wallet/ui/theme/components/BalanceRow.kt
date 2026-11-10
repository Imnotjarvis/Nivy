package com.nivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nivy.design.system.typography.numberHeadlineLarge
import com.nivy.wallet.NivyComponentPreview
import com.nivy.wallet.utils.HiddenAmount
import com.nivy.wallet.utils.formatAmount

@Composable
fun BalanceRowMedium(
    currency: String,
    balance: Double,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    balanceAmountPrefix: String? = null,
    shortenBigNumbers: Boolean = false,
    hiddenMode: Boolean = false,
) {
    BalanceRow(
        modifier = modifier,

        textColor = textColor,
        currency = currency,
        balance = balance,
        hiddenMode = hiddenMode,
        balanceFontSize = 26.sp,

        balanceAmountPrefix = balanceAmountPrefix,
        shortenBigNumbers = shortenBigNumbers
    )
}

@Composable
fun BalanceRowMini(
    currency: String,
    balance: Double,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    balanceAmountPrefix: String? = null,
    shortenBigNumbers: Boolean = false,
    hiddenMode: Boolean = false,
) {
    BalanceRow(
        modifier = modifier,

        textColor = textColor,
        currency = currency,
        balance = balance,
        hiddenMode = hiddenMode,
        balanceFontSize = 22.sp,

        balanceAmountPrefix = balanceAmountPrefix,
        shortenBigNumbers = shortenBigNumbers
    )
}

@Composable
fun BalanceRow(
    currency: String,
    balance: Double,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    hiddenMode: Boolean = false,
    balanceFontSize: TextUnit? = null,
    balanceAmountPrefix: String? = null,
    shortenBigNumbers: Boolean = false,
) {
    val integerPartFormatted = formatAmount(balance, currency, shorten = shortenBigNumbers)

    val balanceText = when {
        hiddenMode -> HiddenAmount
        balanceAmountPrefix != null -> "$balanceAmountPrefix$integerPartFormatted"
        else -> integerPartFormatted
    }

    // The currency after the amount, as everywhere else in the app.
    val balanceCurrencyText = "$balanceText $currency"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = balanceCurrencyText,
            style = if (balanceFontSize == null) {
                MaterialTheme.typography.numberHeadlineLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                MaterialTheme.typography.numberHeadlineLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold
                ).copy(fontSize = balanceFontSize)
            }
        )
    }
}

@Preview
@Composable
private fun Preview_Default() {
    NivyComponentPreview {
        BalanceRow(
            textColor = MaterialTheme.colorScheme.onSurface,
            currency = "BGN",
            balance = 3520000.60,
            balanceAmountPrefix = null,
            shortenBigNumbers = true
        )
    }
}

@Preview
@Composable
private fun Preview_Medium() {
    NivyComponentPreview {
        BalanceRowMedium(
            textColor = MaterialTheme.colorScheme.onSurface,
            currency = "BGN",
            balance = 3520.60,
            balanceAmountPrefix = null
        )
    }
}

@Preview
@Composable
private fun Preview_Mini() {
    NivyComponentPreview {
        BalanceRowMini(
            textColor = MaterialTheme.colorScheme.onSurface,
            currency = "BGN",
            balance = 3520.60,
            balanceAmountPrefix = null,
            shortenBigNumbers = true
        )
    }
}
