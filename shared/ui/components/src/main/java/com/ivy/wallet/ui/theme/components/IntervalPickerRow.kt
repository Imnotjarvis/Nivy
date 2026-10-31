package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.IntervalType
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.wallet.forDisplay
import com.ivy.wallet.utils.capitalizeLocal
import com.ivy.wallet.utils.isNotNullOrBlank
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.White
import androidx.compose.material3.MaterialTheme

private const val RepeatIntervalCharLimit = 5

@Composable
fun IntervalPickerRow(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalN: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSetIntervalType: (IntervalType) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        var interNTextFieldValue by remember(intervalN) {
            mutableStateOf(selectEndTextFieldValue(intervalN.toString()))
        }

        val validInput = intervalN > 0 && interNTextFieldValue.text.isNotNullOrBlank()

        IvyNumberTextField(
            modifier = Modifier
                .background(
                    brush = if (validInput) {
                        GradientIvy.asHorizontalBrush()
                    } else {
                        Gradient
                            .solid(MaterialTheme.colorScheme.surfaceVariant)
                            .asHorizontalBrush()
                    },
                    shape = CircleShape
                )
                .padding(vertical = 12.dp),
            value = interNTextFieldValue,
            textColor = if (validInput) White else MaterialTheme.colorScheme.onSurface,
            hint = "0"
        ) {
            val filteredText = it.text.take(RepeatIntervalCharLimit)
            if (it.text != interNTextFieldValue.text) {
                filteredText.toIntOrNull()?.let(onSetIntervalN)
            }
            interNTextFieldValue = it.copy(text = filteredText)
        }

        Spacer(Modifier.width(12.dp))

        IntervalTypeSelector(
            intervalN = intervalN,
            intervalType = intervalType
        ) {
            onSetIntervalType(it)
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun RowScope.IntervalTypeSelector(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalType: (IntervalType) -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        IvyIcon(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    onSetIntervalType(
                        when (intervalType) {
                            IntervalType.DAY -> IntervalType.YEAR
                            IntervalType.WEEK -> IntervalType.DAY
                            IntervalType.MONTH -> IntervalType.WEEK
                            IntervalType.YEAR -> IntervalType.MONTH
                        }
                    )
                }
                .padding(all = 8.dp)
                .rotate(-180f),
            icon = R.drawable.ic_arrow_right,
            contentDescription = "interval_type_arrow_left"
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = intervalType.forDisplay(intervalN).capitalizeLocal(),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.weight(1f))

        IvyIcon(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    onSetIntervalType(
                        when (intervalType) {
                            IntervalType.DAY -> IntervalType.WEEK
                            IntervalType.WEEK -> IntervalType.MONTH
                            IntervalType.MONTH -> IntervalType.YEAR
                            IntervalType.YEAR -> IntervalType.DAY
                        }
                    )
                }
                .padding(all = 8.dp),
            icon = R.drawable.ic_arrow_right,
            contentDescription = "interval_type_arrow_right"
        )

        Spacer(Modifier.width(20.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IntervalPickerRow(
            intervalN = 1,
            intervalType = IntervalType.WEEK,
            onSetIntervalN = {}
        ) {
        }
    }
}
