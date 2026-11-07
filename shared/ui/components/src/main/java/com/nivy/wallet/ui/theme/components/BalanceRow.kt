package com.nivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.shortenAmount
import com.nivy.wallet.utils.shouldShortAmount
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberHeadlineLarge

@Composable
fun BalanceRowMedium(
    currency: String,
    balance: Double,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    balanceAmountPrefix: String? = null,
    currencyUpfront: Boolean = true,
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
        currencyUpfront = currencyUpfront,
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
    currencyUpfront: Boolean = true,
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
        currencyUpfront = currencyUpfront,
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
    currencyUpfront: Boolean = true,
    balanceAmountPrefix: String? = null,
    shortenBigNumbers: Boolean = false,
) {
    val shortAmount = shortenBigNumbers && shouldShortAmount(balance)
    val integerPartFormatted = if (shortAmount) {
        shortenAmount(balance)
    } else {
        balance.format(currency)
    }

    val balanceText = when {
        hiddenMode -> "****"
        balanceAmountPrefix != null -> "$balanceAmountPrefix$integerPartFormatted"
        else -> integerPartFormatted
    }

    val balanceCurrencyText = if (currencyUpfront) {
        "$currency $balanceText"
    } else {
        "$balanceText $currency"
    }

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
