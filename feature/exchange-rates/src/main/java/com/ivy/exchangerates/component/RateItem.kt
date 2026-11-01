package com.ivy.exchangerates.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.exchangerates.data.RateUi
import com.ivy.ui.R
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.wallet.utils.format
import com.ivy.wallet.ui.theme.components.DeleteButton
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.design.system.typography.numberTitleLarge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

@Composable
fun RateItem(
    rate: RateUi,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(
                horizontal = 16.dp
            )
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {
            val currencyValue: Double = 1.0
            RateColumn(
                label = stringResource(R.string.sell),
                rate = rate.from,
                value = currencyValue.format(currencyCode = rate.from)
            )

            Spacer(Modifier.width(16.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
            Spacer(Modifier.width(16.dp))
            RateColumn(
                label = stringResource(R.string.buy),
                rate = rate.to,
                value = rate.rate.format(currencyCode = rate.to)
            )

            if (onDelete != null) {
                Spacer(Modifier.weight(1f))
                DeleteButton(onClick = onDelete)
            }
        }
    }
}

@Composable
private fun RateColumn(label: String, rate: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
        Text(
            text = rate,
            style = MaterialTheme.typography.numberTitleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

// region Preview
@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        RateItem(
            rate = RateUi(
            from = "BGN",
            to = "EUR",
            rate = 1.95583
        ),
            onDelete = null,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun Preview_Delete() {
    IvyWalletComponentPreview {
        RateItem(
            rate = RateUi(
                from = "BGN",
                to = "EUR",
                rate = 1.95583
        ),
            onDelete = { },
            onClick = {}
        )
    }
}
// endregion
