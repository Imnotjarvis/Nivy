package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.design.utils.drawColoredShadow
import com.ivy.design.utils.thenIf
import com.ivy.ui.R
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GradientIvy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

@Composable
fun OnboardingButton(
    text: String,
    textColor: Color,
    backgroundGradient: Gradient,
    modifier: Modifier = Modifier,
    @DrawableRes iconStart: Int? = null,
    hasNext: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .thenIf(enabled) {
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
                    backgroundGradient.asHorizontalBrush()
                } else {
                    SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        if (iconStart != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 8.dp)
                    .padding(start = 24.dp),
                painter = painterResource(id = iconStart),
                contentDescription = "iconStart"
            )
        }

        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )

        if (hasNext && enabled) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 8.dp)
                    .padding(end = 24.dp),
                painter = painterResource(id = R.drawable.ic_onboarding_next_arrow),
                contentDescription = "next"
            )
        }
    }
}

@Preview
@Composable
private fun PreviewOnboardingTextField() {
    IvyWalletComponentPreview {
        IvyOutlinedTextField(
            modifier = Modifier.padding(horizontal = 24.dp),
            value = TextFieldValue("name@example.com"),
            hint = "Enter email",
            onValueChanged = {}
        )
    }
}

@Preview
@Composable
private fun PreviewOnboardingButton() {
    IvyWalletComponentPreview {
        OnboardingButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            text = "Login",
            backgroundGradient = GradientIvy,
            hasNext = true,
            textColor = MaterialTheme.colorScheme.surface,
            iconStart = null,
            enabled = false,
            onClick = { }
        )
    }
}
