package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.colors.readableTint

@Composable
fun NivyButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Nivy,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = White,
        fontWeight = FontWeight.Bold
    ),
    @DrawableRes iconStart: Int? = null,
    @DrawableRes iconEnd: Int? = null,
    iconTint: Color = White,
    enabled: Boolean = true,
    wrapContentMode: Boolean = true,
    padding: Dp = 12.dp,
    iconEdgePadding: Dp = 12.dp,
    iconTextPadding: Dp = 4.dp,
    onClick: () -> Unit
) {
    // The fill drawn: content is readable on it, also on a disabled button's grey.
    val fill = if (enabled) backgroundColor else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(
                color = fill,
                shape = PillShape
            )
            .clickable(onClick = onClick, enabled = enabled),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            iconStart != null -> {
                IconStart(
                    icon = iconStart,
                    tint = readableTint(fill, iconTint),
                    iconEdgePadding = iconEdgePadding,
                    iconTextPadding = iconTextPadding
                )
            }
            iconEnd != null && !wrapContentMode -> {
                IconEnd(
                    icon = iconEnd,
                    tint = Color.Transparent,
                    iconEdgePadding = iconEdgePadding,
                    iconTextPadding = iconTextPadding
                )
            }
            else -> {
                Spacer(modifier = Modifier.width(24.dp))
            }
        }

        if (!wrapContentMode) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            modifier = Modifier.padding(
                vertical = padding,
            ),
            text = text,
            style = textStyle.copy(color = readableTint(fill, textStyle.color))
        )

        if (!wrapContentMode) {
            Spacer(modifier = Modifier.weight(1f))
        }

        when {
            iconStart != null && !wrapContentMode -> {
                IconStart(
                    icon = iconStart,
                    tint = Color.Transparent,
                    iconEdgePadding = iconEdgePadding,
                    iconTextPadding = iconTextPadding
                )
            }
            iconEnd != null -> {
                IconEnd(
                    icon = iconEnd,
                    tint = readableTint(fill, iconTint),
                    iconEdgePadding = iconEdgePadding,
                    iconTextPadding = iconTextPadding
                )
            }
            else -> {
                Spacer(modifier = Modifier.width(24.dp))
            }
        }
    }
}

@Composable
private fun IconStart(
    iconEdgePadding: Dp,
    iconTextPadding: Dp,
    icon: Int,
    tint: Color,
) {
    Spacer(modifier = Modifier.width(iconEdgePadding))

    Icon(
        modifier = Modifier,
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = tint,
    )

    Spacer(modifier = Modifier.width(iconTextPadding))
}

@Composable
private fun IconEnd(
    iconEdgePadding: Dp,
    iconTextPadding: Dp,
    icon: Int,
    tint: Color,
) {
    Spacer(modifier = Modifier.width(iconTextPadding))

    Icon(
        modifier = Modifier,
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = tint,
    )

    Spacer(modifier = Modifier.width(iconEdgePadding))
}

@Preview
@Composable
private fun PreviewNivyButtonWrapContentWithIconStart() {
    NivyComponentPreview {
        NivyButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .wrapContentSize(),
            iconStart = R.drawable.ic_plus,
            text = "Add new",
            wrapContentMode = true
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewNivyButtonFillMaxWidthWithIconStart() {
    NivyComponentPreview {
        NivyButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            iconStart = R.drawable.ic_plus,
            text = "Add new",
            wrapContentMode = false
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewNivyButtonWrapContentWithIconEnd() {
    NivyComponentPreview {
        NivyButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .wrapContentSize(),
            backgroundColor = Nivy,
            iconEnd = R.drawable.ic_onboarding_next_arrow,
            text = "Category 1",
            wrapContentMode = true
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewNivyButtonFillMaxWidthWithIconEnd() {
    NivyComponentPreview {
        NivyButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            backgroundColor = Nivy,
            iconEnd = R.drawable.ic_onboarding_next_arrow,
            text = "Category 1",
            wrapContentMode = false
        ) {
        }
    }
}
