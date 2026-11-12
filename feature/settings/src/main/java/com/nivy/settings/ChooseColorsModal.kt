package com.nivy.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.ColorPalette
import com.nivy.design.system.colors.Black
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.colors
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import java.util.UUID

/**
 * The Colors setting's choices, each with its colors: Nivy's palette, the wallpaper's where
 * Android gives its colors, the palettes of several colors, the single accents, and minimal
 * colors. Choosing one closes it.
 */
@Composable
internal fun BoxWithConstraintsScope.ChooseColorsModal(
    visible: Boolean,
    dismiss: () -> Unit,
    onChoosePalette: (ColorPalette) -> Unit,
    onChooseMinimal: () -> Unit,
) {
    NivyModal(id = ModalId, visible = visible, dismiss = dismiss, primaryAction = { }) {
        Spacer(Modifier.height(32.dp))
        ModalTitle(text = stringResource(R.string.colors))
        Spacer(Modifier.height(12.dp))

        val choose = { palette: ColorPalette ->
            onChoosePalette(palette)
            dismiss()
        }
        PaletteOption(ColorPalette.NIVY, onChoose = choose)
        if (ColorMode.wallpaper != null) {
            PaletteOption(
                palette = ColorPalette.WALLPAPER,
                subtitle = stringResource(R.string.colors_from_wallpaper),
                onChoose = choose,
            )
        }
        Section(stringResource(R.string.colors_palettes))
        Palettes.forEach { PaletteOption(it, onChoose = choose) }
        Section(stringResource(R.string.colors_accents))
        Accents.forEach { PaletteOption(it, onChoose = choose) }

        Spacer(Modifier.height(8.dp))
        Option(
            name = stringResource(R.string.colors_minimal),
            subtitle = stringResource(R.string.colors_minimal_description),
            swatches = listOf(if (ColorMode.dark) White else Black, Gray),
            selected = ColorMode.minimal,
            tag = "colors_minimal",
        ) {
            onChooseMinimal()
            dismiss()
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The palette's name, as the settings show it. */
@Composable
@ReadOnlyComposable
internal fun ColorPalette.label(): String = stringResource(
    when (this) {
        ColorPalette.NIVY -> R.string.palette_nivy
        ColorPalette.WALLPAPER -> R.string.palette_wallpaper
        ColorPalette.OCEAN -> R.string.palette_ocean
        ColorPalette.SUNSET -> R.string.palette_sunset
        ColorPalette.SKY -> R.string.palette_sky
        ColorPalette.AMBER -> R.string.palette_amber
        ColorPalette.PINK -> R.string.palette_pink
        ColorPalette.VIOLET -> R.string.palette_violet
        ColorPalette.GRAPHITE -> R.string.palette_graphite
    }
)

// A palette's row: its colors as the current theme draws them, as they are in minimal colors too.
@Composable
private fun PaletteOption(palette: ColorPalette, subtitle: String? = null, onChoose: (ColorPalette) -> Unit) {
    val colors = palette.colors()
    val swatches = when (palette) {
        in Accents -> listOf(colors.accent, colors.third)
        // Its accent is its second color too.
        ColorPalette.WALLPAPER -> listOf(colors.accent, colors.third, colors.fourth)
        else -> listOf(colors.accent, colors.second, colors.third, colors.fourth)
    }
    Option(
        name = palette.label(),
        subtitle = subtitle,
        swatches = swatches.map { it.tone },
        selected = !ColorMode.minimal && ColorMode.palette == palette,
        tag = "colors_${palette.name}",
    ) {
        onChoose(palette)
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp),
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(color = Gray, fontWeight = FontWeight.Bold),
    )
}

// The chosen one on the cards' grey, outlined in the accent: filled with the accent, it would hide
// the accent's own swatch.
@Composable
private fun Option(
    name: String,
    subtitle: String?,
    swatches: List<Color>,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .border(SelectedOutline, if (selected) Nivy else Color.Transparent, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Gray))
            }
        }
        swatches.forEach { swatch ->
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(SwatchSize).background(swatch, CircleShape))
        }
    }
}

// The palettes of several colors, and the single accents.
private val Palettes = listOf(ColorPalette.OCEAN, ColorPalette.SUNSET)
private val Accents = listOf(
    ColorPalette.SKY,
    ColorPalette.AMBER,
    ColorPalette.PINK,
    ColorPalette.VIOLET,
    ColorPalette.GRAPHITE,
)

private val SelectedOutline = 2.dp
private val SwatchSize = 20.dp
private val ModalId = UUID.randomUUID()
