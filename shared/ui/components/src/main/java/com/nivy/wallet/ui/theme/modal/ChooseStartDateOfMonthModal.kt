package com.nivy.wallet.ui.theme.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.thenIf
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge

@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.ChooseStartDateOfMonthModal(
    visible: Boolean,
    selectedStartDateOfMonth: Int,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.choose_start_date_of_month),

    id: UUID = UUID.randomUUID(),
    onStartDateOfMonthSelected: (Int) -> Unit,
) {
    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = { }
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(32.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 1,
            toInclusive = 5
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 6,
            toInclusive = 10
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 11,
            toInclusive = 15
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 16,
            toInclusive = 20
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 21,
            toInclusive = 25
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 26,
            toInclusive = 30
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 31,
            toInclusive = 31,
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun save(
    number: Int,

    onStartDateOfMonthSelected: (Int) -> Unit,
    dismiss: () -> Unit
) {
    onStartDateOfMonthSelected(number)
    dismiss()
}

@Composable
private fun ColumnScope.NumberRow(
    selectedNumber: Int,
    fromInclusive: Int,
    toInclusive: Int,
    onClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        for (number in fromInclusive..toInclusive) {
            NumberView(
                number = number,
                selected = number == selectedNumber
            ) {
                onClick(it)
            }

            Spacer(Modifier.width(20.dp))
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun NumberView(
    number: Int,
    selected: Boolean,
    onClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(PillShape)
            .border(2.dp, if (selected) Nivy else MaterialTheme.colorScheme.surfaceVariant, PillShape)
            .thenIf(selected) {
                background(Nivy, PillShape)
            }
            .clickable {
                onClick(number)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                // Readable on the fill, also where minimal colors turn it into the text color.
                color = if (selected) readableTint(Nivy, White) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        ChooseStartDateOfMonthModal(
            visible = true,
            selectedStartDateOfMonth = 1,
            dismiss = {}
        ) {
        }
    }
}
