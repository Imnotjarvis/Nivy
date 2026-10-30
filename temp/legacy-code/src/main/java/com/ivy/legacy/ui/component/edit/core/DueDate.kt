package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.ivy.legacy.LocalTimeFormatter
import com.ivy.legacy.LocalTimeProvider
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.ui.theme.components.IvyIcon
import java.time.Instant
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun DueDate(
    dueDate: Instant,
    onPickDueDate: () -> Unit,
) {
    DueDateCard(
        dueDate = dueDate,
        onClick = {
            onPickDueDate()
        }
    )
}

@Composable
private fun DueDateCard(
    dueDate: Instant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIcon(icon = R.drawable.ic_planned_payments)

        Spacer(Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.planned_for),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = with(LocalTimeFormatter.current) {
                dueDate.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = false))
            },
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_OneTime() {
    IvyWalletComponentPreview {
        DueDate(
            dueDate = LocalTimeProvider.current.utcNow()
                .plusSeconds(TimeUnit.DAYS.toSeconds(6)),
        ) {
        }
    }
}
