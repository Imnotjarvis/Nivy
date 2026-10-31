package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.utils.thenIf
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.design.system.colors.Green
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

@Composable
fun IvyOutlinedButton(
    text: String,
    @DrawableRes iconStart: Int?,
    modifier: Modifier = Modifier,
    solidBackground: Boolean = false,
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    padding: Dp = 12.dp,
    onClick: () -> Unit,
) {
    val pure = MaterialTheme.colorScheme.surface
    val rFull = CircleShape
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
            )
            .defaultMinSize(minWidth, minHeight)
            .border(2.dp, borderColor, CircleShape)
            .thenIf(solidBackground) {
                background(pure, rFull)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (iconStart != null) {
            Spacer(Modifier.width(12.dp))

            IvyIcon(
                icon = iconStart,
                tint = iconTint,
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(24.dp))
        }

        Text(
            modifier = Modifier.padding(vertical = padding, horizontal = 4.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            ),
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
fun IvyOutlinedButtonFillMaxWidth(
    text: String,
    @DrawableRes iconStart: Int?,
    modifier: Modifier = Modifier,
    solidBackground: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    padding: Dp = 16.dp,
    onClick: () -> Unit,
) {
    val pure = MaterialTheme.colorScheme.surface
    val rFull = CircleShape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
            )
            .border(2.dp, borderColor, CircleShape)
            .thenIf(solidBackground) {
                background(pure, rFull)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconStart != null) {
            Spacer(Modifier.width(12.dp))

            IvyIcon(
                icon = iconStart,
                tint = iconTint,
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            modifier = Modifier.padding(vertical = padding),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            ),
        )

        Spacer(Modifier.weight(1f))

        if (iconStart != null) {
            Spacer(Modifier.width(12.dp))

            IvyIcon(
                icon = iconStart,
                tint = Color.Transparent,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_FillMaxWidth() {
    IvyWalletComponentPreview {
        IvyOutlinedButtonFillMaxWidth(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Import backup file",
            iconStart = R.drawable.ic_export_csv,
            textColor = Green,
            iconTint = Green,
        ) {}
    }
}
