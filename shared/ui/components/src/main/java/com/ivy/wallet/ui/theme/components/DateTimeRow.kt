package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import java.time.LocalDateTime

@Composable
fun DateTimeRow(
    dateTime: LocalDateTime,
    onEditDate: () -> Unit,
    onEditTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = LocalTimeFormatter.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        IvyOutlinedButton(
            text = with(timeFormatter) {
                dateTime.format(TimeFormatter.Style.DateOnly(includeWeekDay = true))
            },
            iconStart = R.drawable.ic_date,
            onClick = onEditDate
        )

        Spacer(Modifier.weight(1f))

        IvyOutlinedButton(
            text = with(timeFormatter) {
                dateTime.toLocalTime().format()
            },
            iconStart = R.drawable.ic_date,
            onClick = onEditTime
        )

        Spacer(Modifier.width(24.dp))
    }
}
