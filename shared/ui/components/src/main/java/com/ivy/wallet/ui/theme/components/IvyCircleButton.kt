package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.design.utils.drawColoredShadow
import com.ivy.design.utils.thenIf
import com.ivy.ui.R
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.GradientRed
import com.ivy.design.system.colors.White
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

@Composable
fun IvyCircleButton(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    backgroundPadding: Dp = 0.dp,
    backgroundGradient: Gradient = GradientIvy,
    horizontalGradient: Boolean = true,
    tint: Color = White,
    enabled: Boolean = true,
    hasShadow: Boolean = true,
    onClick: () -> Unit
) {
    IvyIcon(
        modifier = modifier
            .thenIf(enabled && hasShadow) {
                drawColoredShadow(
                    color = backgroundGradient.startColor,
                    borderRadius = 0.dp,
                    shadowRadius = 16.dp,
                    offsetX = 0.dp,
                    offsetY = 8.dp
                )
            }
            .clip(CircleShape)
            .background(
                brush = if (enabled) {
                    if (horizontalGradient) {
                        backgroundGradient.asHorizontalBrush()
                    } else {
                        backgroundGradient.asVerticalBrush()
                    }
                } else {
                    SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick, enabled = enabled)
            .padding(all = backgroundPadding),
        icon = icon,
        tint = tint,
        contentDescription = "circle button"
    )
}

@Preview
@Composable
private fun PreviewIvyCircleButton_Enabled() {
    IvyWalletComponentPreview {
        IvyCircleButton(
            icon = R.drawable.ic_delete,
            backgroundGradient = GradientRed,
            tint = White
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewIvyCircleButton_Disabled() {
    IvyWalletComponentPreview {
        IvyCircleButton(
            icon = R.drawable.ic_delete,
            backgroundGradient = GradientRed,
            enabled = false,
            tint = White
        ) {
        }
    }
}
