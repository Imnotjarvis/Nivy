package com.ivy.design.l2_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.White
import com.ivy.design.l1_buildingBlocks.data.Background
import com.ivy.design.l1_buildingBlocks.data.background
import com.ivy.design.l1_buildingBlocks.data.clipBackground
import com.ivy.design.utils.IvyComponentPreview
import com.ivy.design.utils.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun Button(
    modifier: Modifier = Modifier,
    text: String,
    background: Background = Background.Solid(
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        padding = padding(
            horizontal = 24.dp,
            vertical = 12.dp
        )
    ),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    ),
    onClick: () -> Unit
) {
    Text(
        modifier = modifier
            .clipBackground(background)
            .clickable(
                onClick = onClick
            )
            .background(background),
        text = text,
        style = textStyle
    )
}

@Preview
@Composable
private fun Preview_Solid() {
    IvyComponentPreview {
        Button(
            text = "Okay",
            background = Background.Solid(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                padding = padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                )
            ),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = White)
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_Outlined() {
    IvyComponentPreview {
        Button(
            text = "Continue",
            background = Background.Outlined(
                color = MaterialTheme.colorScheme.onSurface,
                width = 1.dp,
                shape = CircleShape,
                padding = padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                )
            ),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_FillMaxWidth() {
    IvyComponentPreview {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = "Add task"
        ) {
        }
    }
}
