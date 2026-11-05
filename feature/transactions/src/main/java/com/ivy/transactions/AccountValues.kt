package com.ivy.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.expenseRed
import com.ivy.design.system.colors.incomeGreen
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.formatGainPercent

/** A value entered: its day and the money put in by then, its amount and the gain on that money. */
@Composable
internal fun ValueEntryRow(
    entry: ValueEntry,
    currency: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val value = entry.value.value.value
    val gain = value - entry.invested
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("account_value_entry"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = with(LocalTimeFormatter.current) {
                    entry.value.time.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = stringResource(R.string.invested_amount, "${entry.invested.format(currency)} $currency"),
                style = MaterialTheme.typography.bodySmall.copy(color = Gray, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${value.format(currency)} $currency",
                maxLines = 1,
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            formatGainPercent(gain, entry.invested)?.let {
                // Green and red as on Home's cards, also in minimal colors.
                val panel = MaterialTheme.colorScheme.surfaceVariant
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (gain < 0) expenseRed(panel) else incomeGreen(panel),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}
