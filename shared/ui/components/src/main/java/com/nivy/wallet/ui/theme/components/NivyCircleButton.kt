package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Red
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme

@Composable
fun NivyCircleButton(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    backgroundPadding: Dp = 0.dp,
    backgroundColor: Color = Nivy,
    tint: Color = White,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    // The fill drawn: the icon is readable on it, also on a disabled button's grey.
    val fill = if (enabled) backgroundColor else MaterialTheme.colorScheme.onSurfaceVariant
    NivyIcon(
        modifier = modifier
            .clip(PillShape)
            .background(
                color = fill,
                shape = PillShape
            )
            .clickable(onClick = onClick, enabled = enabled)
            .padding(all = backgroundPadding),
        icon = icon,
        tint = readableTint(fill, tint),
        contentDescription = contentDescription
    )
}

@Preview
@Composable
private fun PreviewNivyCircleButton_Enabled() {
    NivyComponentPreview {
        NivyCircleButton(
            icon = R.drawable.ic_delete,
            backgroundColor = Red,
            tint = White
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewNivyCircleButton_Disabled() {
    NivyComponentPreview {
        NivyCircleButton(
            icon = R.drawable.ic_delete,
            backgroundColor = Red,
            enabled = false,
            tint = White
        ) {
        }
    }
}
