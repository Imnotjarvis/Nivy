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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_COLORS_FREE
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_COLORS_PREMIUM
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_GREYS
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.design.system.colors.greyscale
import com.nivy.design.utils.densityScope
import com.nivy.design.utils.thenIf
import com.nivy.ui.R
import kotlinx.coroutines.launch
import kotlin.math.abs
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

    // In minimal colors, the greys: a color saved before shows as the grey nearest its own, which
    // stays saved until another is picked.
    val nivyColors = if (ColorMode.minimal) {
        NIVY_COLOR_PICKER_GREYS.map { NivyColor(color = it, premium = false) }
    } else {
        freeNivyColors + premiumNivyColors
    }
    val shownColor = if (ColorMode.minimal) nearestGrey(selectedColor) else selectedColor

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    densityScope {
        LaunchedEffect(Unit) {
            // java.lang.IllegalStateException: pending composition has not been applied

            val selectedColorIndex = nivyColors.indexOfFirst { it.color == shownColor }
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
                selectedColor = shownColor,
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
    val minimal = ColorMode.minimal
    val ink = MaterialTheme.colorScheme.onSurface

    if (index == 0) {
        Spacer(Modifier.width(24.dp))
    }

    Box(
        modifier = Modifier
            .clip(PillShape)
            .size(48.dp)
            // In minimal colors the grey picked is ringed in the text color, apart from it: a ring
            // on the swatch itself could match it, or the sheet around it.
            .thenIf(minimal && selected) {
                border(width = 3.dp, color = ink, PillShape).padding(6.dp)
            }
            .background(color, PillShape)
            // The greys near the sheet's own color would fade into it: a faint ring keeps each.
            .thenIf(minimal) {
                border(width = 1.dp, color = Gray.copy(alpha = OutlineAlpha), PillShape)
            }
            .thenIf(!minimal && selected) {
                border(width = 4.dp, color = color.dynamicContrast(), PillShape)
            }
            .clickable(onClick = {
                onSelected(nivyColor)
            })
            .testTag("color_item_${nivyColor.color.value}"),
    )

    Spacer(Modifier.width(if (selected) 16.dp else 24.dp))
}

// Of the greys offered, the one nearest [color]'s own grey.
private fun nearestGrey(color: Color): Color {
    val lightness = color.greyscale().luminance()
    return NIVY_COLOR_PICKER_GREYS.minBy { abs(it.luminance() - lightness) }
}

private const val OutlineAlpha = 0.5f
