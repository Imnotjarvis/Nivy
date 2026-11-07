package com.nivy.wallet.ui.theme.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.readableTint
import com.nivy.design.system.colors.White
import androidx.compose.ui.graphics.ColorFilter
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme

@Composable
fun OnboardingButton(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconStart: Int? = null,
    hasNext: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Readable on the fill drawn, also on a disabled button's grey.
    val fill = if (enabled) backgroundColor else MaterialTheme.colorScheme.onSurfaceVariant
    val content = readableTint(fill, textColor)
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(
                color = fill,
                shape = PillShape
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
                contentDescription = null
            )
        }

        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = content,
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
                colorFilter = ColorFilter.tint(readableTint(fill, White)),
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun PreviewOnboardingTextField() {
    NivyComponentPreview {
        NivyOutlinedTextField(
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
    NivyComponentPreview {
        OnboardingButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            text = "Login",
            backgroundColor = Nivy,
            hasNext = true,
            textColor = MaterialTheme.colorScheme.surface,
            iconStart = null,
            enabled = false,
            onClick = { }
        )
    }
}
