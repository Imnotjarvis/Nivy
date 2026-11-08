package com.nivy.transaction

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import com.nivy.design.system.shapes.PillShape
import com.nivy.wallet.ui.theme.components.NivyIcon
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import com.nivy.design.system.typography.numberTitleSmall
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.design.system.colors.Gray
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.ui.R
import java.time.LocalDateTime
import androidx.compose.material3.MaterialTheme

/**
 * When the credit card charges the expense, with arrows to move it a month earlier or later, like
 * when the bank closed the statement on another day. Like the date row above it: the label in the
 * text font, the date in the number font.
 */
@Composable
internal fun ChargeDate(
    chargeDate: LocalDateTime,
    canMoveEarlier: Boolean,
    onMove: (months: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 32.dp, end = 16.dp)
            .testTag("transaction_charge_date"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.charged_on),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = with(LocalTimeFormatter.current) {
                chargeDate.format(TimeFormatter.Style.DateOnly(includeWeekDay = true))
            },
            style = MaterialTheme.typography.numberTitleSmall.copy(color = Gray),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MoveChargeButton(
            modifier = Modifier.rotate(180f),
            description = stringResource(R.string.charge_earlier),
            enabled = canMoveEarlier,
            testTag = "transaction_charge_earlier",
            onClick = { onMove(-1) },
        )
        MoveChargeButton(
            description = stringResource(R.string.charge_later),
            enabled = true,
            testTag = "transaction_charge_later",
            onClick = { onMove(1) },
        )
    }
}

@Composable
private fun MoveChargeButton(
    description: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NivyIcon(
        modifier = modifier
            .size(40.dp)
            .clip(PillShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
            .testTag(testTag),
        icon = R.drawable.ic_arrow_right,
        tint = if (enabled) MaterialTheme.colorScheme.onSurface else Gray.copy(alpha = DisabledAlpha),
        contentDescription = description,
    )
}

private const val DisabledAlpha = 0.4f
