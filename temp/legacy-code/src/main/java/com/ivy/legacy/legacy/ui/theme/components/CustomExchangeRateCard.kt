package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.design.system.colors.Orange
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge

@Composable
fun CustomExchangeRateCard(
    fromCurrencyCode: String,
    toCurrencyCode: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.exchange_rate),
    onRefresh: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIcon(icon = R.drawable.ic_currency)

        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fromCurrencyCode,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Orange,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                IvyIcon(icon = R.drawable.ic_arrow_right, tint = Orange)
                Text(
                    text = toCurrencyCode,
                    style = MaterialTheme.typography.numberBodyLarge.copy(
                        color = Orange,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.numberBodyLarge.copy(
                        color = Orange,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                IvyIcon(icon = R.drawable.ic_arrow_right, tint = Orange)
                Text(
                    text = exchangeRate.format(IvyCurrency.getDecimalPlaces(toCurrencyCode)),
                    style = MaterialTheme.typography.numberBodyLarge.copy(
                        color = Orange,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
        IvyIcon(
            icon = R.drawable.ic_refresh,
            modifier = Modifier
                .padding(end = 16.dp)
                .clickable {
                    onRefresh()
                }
        )
    }
}

@Preview
@Composable
private fun Preview_OneTime() {
    IvyWalletComponentPreview {
        CustomExchangeRateCard(
            fromCurrencyCode = "INR",
            toCurrencyCode = "EUR",
            exchangeRate = (86.2)
        ) {
        }
    }
}
