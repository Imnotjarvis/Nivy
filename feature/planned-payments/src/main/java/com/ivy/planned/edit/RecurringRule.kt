package com.ivy.planned.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.ivy.base.model.IntervalType
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.wallet.forDisplay
import com.ivy.wallet.utils.formatLocal
import com.ivy.wallet.utils.uppercaseLocal
import com.ivy.ui.R
import com.ivy.design.system.colors.Orange
import com.ivy.wallet.ui.theme.components.AddPrimaryAttributeButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import java.time.LocalDateTime
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge

@Composable
fun RecurringRule(
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
    onShowRecurringRuleModal: () -> Unit,
) {
    if (
        hasRecurringRule(
            startDate = startDate,
            intervalN = intervalN,
            intervalType = intervalType,
            oneTime = oneTime
        )
    ) {
        RecurringRuleCard(
            startDate = startDate!!,
            intervalN = intervalN,
            intervalType = intervalType,
            oneTime = oneTime,
            onClick = {
                onShowRecurringRuleModal()
            }
        )
    } else {
        AddPrimaryAttributeButton(
            icon = R.drawable.ic_planned_payments,
            text = stringResource(R.string.add_planned_date_payment),
            onClick = onShowRecurringRuleModal
        )
    }
}

fun hasRecurringRule(
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
): Boolean {
    return startDate != null &&
        ((intervalN != null && intervalType != null) || oneTime)
}

@Composable
private fun RecurringRuleCard(
    startDate: LocalDateTime,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
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

        Column {
            Text(
                text = if (oneTime) stringResource(R.string.planned_for) else stringResource(R.string.planned_start_at),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (!oneTime && intervalType != null && intervalN != null) {
                Spacer(Modifier.height(4.dp))

                val intervalTypeLabel = intervalType.forDisplay(intervalN).uppercaseLocal()
                Text(
                    text = stringResource(R.string.repeats_every, intervalN, intervalTypeLabel),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Orange,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = startDate.toLocalDate().formatLocal("MMM. dd"),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview_Empty() {
    IvyWalletComponentPreview {
        RecurringRule(
            startDate = null,
            intervalN = null,
            intervalType = null,
            oneTime = true
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_Repeat() {
    IvyWalletComponentPreview {
        RecurringRule(
            startDate = LocalDateTime.now(),
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            oneTime = false
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_OneTime() {
    IvyWalletComponentPreview {
        RecurringRule(
            startDate = LocalDateTime.now().plusDays(5),
            intervalN = null,
            intervalType = null,
            oneTime = true
        ) {
        }
    }
}
