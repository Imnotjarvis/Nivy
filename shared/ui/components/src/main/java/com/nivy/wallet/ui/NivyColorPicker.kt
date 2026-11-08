package com.nivy.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_COLORS_FREE
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_COLORS_PREMIUM
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.design.utils.densityScope
import com.nivy.design.utils.thenIf
import com.nivy.ui.R
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

private data class NivyColor(
    val color: Color,
    val premium: Boolean
)

@Suppress("ParameterNaming")
@Composable
fun ColumnScope.NivyColorPicker(
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onColorSelected: (Color) -> Unit
) {
    Text(
        modifier = modifier.padding(horizontal = 24.dp),
        text = stringResource(R.string.choose_color),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(16.dp))

    val freeNivyColors = NIVY_COLOR_PICKER_COLORS_FREE
        .map {
            NivyColor(
                color = it,
                premium = false
            )
        }

    val premiumNivyColors = NIVY_COLOR_PICKER_COLORS_PREMIUM
        .map {
            NivyColor(
                color = it,
                premium = true
            )
        }

    val nivyColors = freeNivyColors + premiumNivyColors

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    densityScope {
        LaunchedEffect(Unit) {
            // java.lang.IllegalStateException: pending composition has not been applied

            val selectedColorIndex = nivyColors.indexOfFirst { it.color == selectedColor }
            if (selectedColorIndex != -1) {
                coroutineScope.launch {
                    listState.scrollToItem(
                        index = selectedColorIndex,
                        scrollOffset = 0
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        state = listState
    ) {
        items(
            count = nivyColors.size
        ) { index ->
            ColorItem(
                index = index,
                nivyColor = nivyColors[index],
                selectedColor = selectedColor,
                onSelected = {
                    onColorSelected(it.color)
                }
            )
        }
    }
}

@Composable
@Suppress("ParameterNaming")
private fun ColorItem(
    index: Int,
    nivyColor: NivyColor,
    selectedColor: Color,
    onSelected: (NivyColor) -> Unit
) {
    val color = nivyColor.color
    val selected = color == selectedColor
    val ink = MaterialTheme.colorScheme.onSurface

    if (index == 0) {
        Spacer(Modifier.width(24.dp))
    }

    Box(
        modifier = Modifier
            .clip(PillShape)
            .size(48.dp)
            // The color itself, also in minimal colors: where it's used, it's drawn grey there,
            // but here it's what's being picked.
            .background(color, PillShape)
            .thenIf(selected) {
                // In minimal colors, the text color: it shows on the page whatever the swatch.
                border(width = 4.dp, color = if (ColorMode.minimal) ink else color.dynamicContrast(), PillShape)
            }
            .clickable(onClick = {
                onSelected(nivyColor)
            })
            .testTag("color_item_${nivyColor.color.value}"),
    )

    Spacer(Modifier.width(if (selected) 16.dp else 24.dp))
}
