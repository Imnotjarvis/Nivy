package com.nivy.wallet.ui.theme.wallet

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nivy.design.utils.thenIf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.time.impl.DeviceTimeProvider
import com.nivy.wallet.NivyComponentPreview
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.ui.theme.components.ClearIcon
import com.nivy.wallet.ui.theme.components.NivyIcon
import androidx.compose.material3.MaterialTheme

@Composable
fun PeriodSelector(
    period: TimePeriod,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    // Null where there's no other period to choose: the label then isn't a button.
    onShowChoosePeriodModal: (() -> Unit)?,
    // Null while the period is this month, or where it can't change: no × then.
    onThisMonth: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, PillShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        if (period.month != null) {
            NivyIcon(
                modifier = Modifier
                    .size(48.dp)
                    .clip(PillShape)
                    .clickable {
                        onPreviousMonth()
                    }
                    .padding(all = 8.dp)
                    .rotate(-180f),
                icon = R.drawable.ic_arrow_right
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .height(48.dp)
                .defaultMinSize(minWidth = 48.dp)
                .clip(PillShape)
                .thenIf(onShowChoosePeriodModal != null) {
                    clickable { onShowChoosePeriodModal?.invoke() }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            NivyIcon(
                icon = R.drawable.ic_calendar,
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = period.toDisplayShort(
                    startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
                    timeConverter = LocalTimeConverter.current,
                    timeProvider = LocalTimeProvider.current,
                    timeFormatter = LocalTimeFormatter.current,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )

            if (onThisMonth != null) {
                ClearIcon(onClear = onThisMonth, label = stringResource(R.string.month_this_month))
            }
        }

        Spacer(Modifier.weight(1f))

        if (period.month != null) {
            NivyIcon(
                modifier = Modifier
                    .size(48.dp)
                    .clip(PillShape)
                    .clickable {
                        onNextMonth()
                    }
                    .padding(all = 8.dp),
                icon = R.drawable.ic_arrow_right
            )
        }

        Spacer(Modifier.width(20.dp))
    }
}

/**
 * Goes back to this month, the period the screens start on, through [select]: what a period's ×
 * does. None while [period] is this month, so the × shows only when there's somewhere to go back.
 */
@Composable
@ReadOnlyComposable
fun backToThisMonth(period: TimePeriod, select: (TimePeriod) -> Unit): (() -> Unit)? {
    val thisMonth = TimePeriod.currentMonth(LocalPeriodState.current.startDayOfMonth, LocalTimeProvider.current)
    if (period == thisMonth) return null
    return { select(thisMonth) }
}

@Preview
@Composable
private fun Preview() {
    NivyComponentPreview {
        PeriodSelector(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1,
                timeProvider = DeviceTimeProvider(),
            ), // preview
            onPreviousMonth = { },
            onNextMonth = { },
            onShowChoosePeriodModal = {},
            onThisMonth = null,
        )
    }
}
