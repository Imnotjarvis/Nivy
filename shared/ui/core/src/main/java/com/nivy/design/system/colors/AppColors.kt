package com.nivy.design.system.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.nivy.base.model.ColorPalette

/** A color on light themes and on dark ones. */
@Immutable
data class ThemeTones(val light: Color, val dark: Color) {
    /** As the theme drawing it has it. */
    val tone: Color get() = if (ColorMode.dark) dark else light

    /** For text on [background]: the dark theme's tone on dark backgrounds. */
    fun on(background: Color): Color = if (isDarkColor(background)) dark else light
}

/**
 * The app's own colors: what isn't a background, text, a saved account or category color, or what
 * keeps its meaning (income, expenses, warnings). Each palette's: fills carry content at 4.5:1 and
 * stand 3:1 on the light cards and the dark page; lines and marks stand 3:1 on the cards; text
 * reads at 4.5:1 (see AppColorsTest). They keep Nivy's names in code (Nivy, Green, Orange, Blue).
 */
@Immutable
data class AppColors(
    // Buttons, the +, Home's tab, the charts' main line: Nivy's purple.
    val accent: ThemeTones,
    // Its containers' shades, light and dark.
    val accentLight: Color,
    val accentDark: Color,
    // The Accounts tab and its +, switches, selections: Nivy's green.
    val second: ThemeTones,
    val secondLight: Color,
    val secondDark: Color,
    // Payments to come, a chart's partner line: Nivy's orange.
    val third: ThemeTones,
    val thirdLight: Color,
    val thirdDark: Color,
    val thirdText: ThemeTones,
    // Tags, a chart's other series: Nivy's light blue.
    val fourth: ThemeTones,
    val fourthText: ThemeTones,
)

/** This palette's colors: the wallpaper's as Android gives them, or Nivy's until it does. */
fun ColorPalette.colors(): AppColors = colorsOf(this, ColorMode.wallpaper)

/** The colors of [palette]: for the wallpaper's, its [wallpaper] colors, or Nivy's without them. */
internal fun colorsOf(palette: ColorPalette, wallpaper: AppColors?): AppColors = when (palette) {
    ColorPalette.NIVY -> NivyColors
    ColorPalette.WALLPAPER -> wallpaper ?: NivyColors
    ColorPalette.OCEAN -> OceanColors
    ColorPalette.SUNSET -> SunsetColors
    ColorPalette.SKY -> SkyColors
    ColorPalette.AMBER -> AmberColors
    ColorPalette.PINK -> PinkColors
    ColorPalette.VIOLET -> VioletColors
    ColorPalette.GRAPHITE -> GraphiteColors
}

/**
 * The wallpaper's colors, from Android's [light] and [dark] schemes for it (Android 12 and later):
 * its primary for the accent and the second color, its tertiary and secondary for the rest.
 */
fun wallpaperColors(light: ColorScheme, dark: ColorScheme): AppColors = AppColors(
    accent = ThemeTones(light.primary, dark.primary),
    accentLight = light.primaryContainer,
    accentDark = dark.primaryContainer,
    second = ThemeTones(light.primary, dark.primary),
    secondLight = light.primaryContainer,
    secondDark = dark.primaryContainer,
    // On dark themes the scheme's tertiary is as light as its primary: a chart's partner line, a
    // tone between it and its container, stands apart from the main one.
    third = ThemeTones(light.tertiary, lerp(dark.tertiary, dark.tertiaryContainer, WallpaperPartnerShade)),
    thirdLight = light.tertiaryContainer,
    thirdDark = dark.tertiaryContainer,
    thirdText = ThemeTones(light.tertiary, dark.tertiary),
    fourth = ThemeTones(light.secondary, dark.secondary),
    fourthText = ThemeTones(light.secondary, dark.secondary),
)

private const val WallpaperPartnerShade = 0.5f

// Nivy's own colors. Its green, orange and light blue stood under 2:1 on the light cards: on
// light themes they are deeper shades of the same hues, at 3:1; on dark ones its purple is a
// shade lighter, as far out on the dark cards as white text on it allows. The text tones are the
// darker orange and blue that read at 4.5:1 on light backgrounds, and the hues on dark ones.
internal val NivyColors = AppColors(
    accent = ThemeTones(Color(0xFF6B4DFF), Color(0xFF7155FF)),
    accentLight = Color(0xFFD5CCFF),
    accentDark = Color(0xFF352680),
    second = ThemeTones(Color(0xFF009A70), Color(0xFF14CC9E)),
    secondLight = Color(0xFFAAF2E0),
    secondDark = Color(0xFF0A664F),
    third = ThemeTones(Color(0xFFC47400), Color(0xFFF29F30)),
    thirdLight = Color(0xFFFFDEB3),
    thirdDark = Color(0xFF734B17),
    thirdText = ThemeTones(Color(0xFF9E5A00), Color(0xFFF29F30)),
    fourth = ThemeTones(Color(0xFF0090CA), Color(0xFF4CC3FF)),
    fourthText = ThemeTones(Color(0xFF1A6E9E), Color(0xFFB3E6FF)),
)

// Blue, cerulean, sand and reef pink.
internal val OceanColors = AppColors(
    accent = ThemeTones(Color(0xFF1B73D5), Color(0xFF1B73D5)),
    accentLight = Color(0xFFD2EAFF),
    accentDark = Color(0xFF0C325F),
    second = ThemeTones(Color(0xFF2786BD), Color(0xFF2786BD)),
    secondLight = Color(0xFFCBEDFF),
    secondDark = Color(0xFF00375B),
    third = ThemeTones(Color(0xFFA57610), Color(0xFFA57610)),
    thirdLight = Color(0xFFFAE5C3),
    thirdDark = Color(0xFF492C00),
    thirdText = ThemeTones(Color(0xFF7D5100), Color(0xFFD8B16F)),
    fourth = ThemeTones(Color(0xFFAB67A6), Color(0xFFAB67A6)),
    fourthText = ThemeTones(Color(0xFF7F3E7B), Color(0xFFDBA1D5)),
)

// Magenta, dusk indigo, gold and apricot.
internal val SunsetColors = AppColors(
    accent = ThemeTones(Color(0xFFAF4DA9), Color(0xFFAF4DA9)),
    accentLight = Color(0xFFFCDCF8),
    accentDark = Color(0xFF4B1F48),
    second = ThemeTones(Color(0xFF6A77D3), Color(0xFF6A77D3)),
    secondLight = Color(0xFFDEE6FF),
    secondDark = Color(0xFF272D60),
    third = ThemeTones(Color(0xFF9B7B07), Color(0xFF9B7B07)),
    thirdLight = Color(0xFFF5E7C3),
    thirdDark = Color(0xFF433000),
    thirdText = ThemeTones(Color(0xFF745600), Color(0xFFCFB56D)),
    fourth = ThemeTones(Color(0xFFB56E2B), Color(0xFFB56E2B)),
    fourthText = ThemeTones(Color(0xFF8A4700), Color(0xFFE5A977)),
)

// One accent: a deeper shade of it on light themes and a lighter one on dark ones for the rest.
internal val SkyColors = AppColors(
    accent = ThemeTones(Color(0xFF007DA4), Color(0xFF007DA4)),
    accentLight = Color(0xFFC6EFFF),
    accentDark = Color(0xFF003A55),
    second = ThemeTones(Color(0xFF007DA4), Color(0xFF007DA4)),
    secondLight = Color(0xFFC6EFFF),
    secondDark = Color(0xFF003A55),
    third = ThemeTones(Color(0xFF00526F), Color(0xFF5BB2D2)),
    thirdLight = Color(0xFFC6EFFF),
    thirdDark = Color(0xFF003A55),
    thirdText = ThemeTones(Color(0xFF00526F), Color(0xFF5BB2D2)),
    fourth = ThemeTones(Color(0xFF00526F), Color(0xFF5BB2D2)),
    fourthText = ThemeTones(Color(0xFF00526F), Color(0xFF5BB2D2)),
)

// One accent: a deeper shade of it on light themes and a lighter one on dark ones for the rest.
internal val AmberColors = AppColors(
    accent = ThemeTones(Color(0xFFA86300), Color(0xFFA86300)),
    accentLight = Color(0xFFFFE2C6),
    accentDark = Color(0xFF512600),
    second = ThemeTones(Color(0xFFA86300), Color(0xFFA86300)),
    secondLight = Color(0xFFFFE2C6),
    secondDark = Color(0xFF512600),
    third = ThemeTones(Color(0xFF703E00), Color(0xFFD69C55)),
    thirdLight = Color(0xFFFFE2C6),
    thirdDark = Color(0xFF512600),
    thirdText = ThemeTones(Color(0xFF703E00), Color(0xFFD69C55)),
    fourth = ThemeTones(Color(0xFF703E00), Color(0xFFD69C55)),
    fourthText = ThemeTones(Color(0xFF703E00), Color(0xFFD69C55)),
)

// One accent: a deeper shade of it on light themes and a lighter one on dark ones for the rest.
internal val PinkColors = AppColors(
    accent = ThemeTones(Color(0xFFB84999), Color(0xFFB84999)),
    accentLight = Color(0xFFFFDCF2),
    accentDark = Color(0xFF4F1D41),
    second = ThemeTones(Color(0xFFB84999), Color(0xFFB84999)),
    secondLight = Color(0xFFFFDCF2),
    secondDark = Color(0xFF4F1D41),
    third = ThemeTones(Color(0xFF822169), Color(0xFFE68AC9)),
    thirdLight = Color(0xFFFFDCF2),
    thirdDark = Color(0xFF4F1D41),
    thirdText = ThemeTones(Color(0xFF822169), Color(0xFFE68AC9)),
    fourth = ThemeTones(Color(0xFF822169), Color(0xFFE68AC9)),
    fourthText = ThemeTones(Color(0xFF822169), Color(0xFFE68AC9)),
)

// One accent: a deeper shade of it on light themes and a lighter one on dark ones for the rest.
internal val VioletColors = AppColors(
    accent = ThemeTones(Color(0xFF9356CD), Color(0xFF9356CD)),
    accentLight = Color(0xFFEFE0FF),
    accentDark = Color(0xFF3E2457),
    second = ThemeTones(Color(0xFF9356CD), Color(0xFF9356CD)),
    secondLight = Color(0xFFEFE0FF),
    secondDark = Color(0xFF3E2457),
    third = ThemeTones(Color(0xFF642D94), Color(0xFFC294F6)),
    thirdLight = Color(0xFFEFE0FF),
    thirdDark = Color(0xFF3E2457),
    thirdText = ThemeTones(Color(0xFF642D94), Color(0xFFC294F6)),
    fourth = ThemeTones(Color(0xFF642D94), Color(0xFFC294F6)),
    fourthText = ThemeTones(Color(0xFF642D94), Color(0xFFC294F6)),
)

// One accent: a deeper shade of it on light themes and a lighter one on dark ones for the rest.
internal val GraphiteColors = AppColors(
    accent = ThemeTones(Color(0xFF617689), Color(0xFF617689)),
    accentLight = Color(0xFFD4EBFF),
    accentDark = Color(0xFF223545),
    second = ThemeTones(Color(0xFF617689), Color(0xFF617689)),
    secondLight = Color(0xFFD4EBFF),
    secondDark = Color(0xFF223545),
    third = ThemeTones(Color(0xFF3A4C5B), Color(0xFF97A9B9)),
    thirdLight = Color(0xFFD4EBFF),
    thirdDark = Color(0xFF223545),
    thirdText = ThemeTones(Color(0xFF3A4C5B), Color(0xFF97A9B9)),
    fourth = ThemeTones(Color(0xFF3A4C5B), Color(0xFF97A9B9)),
    fourthText = ThemeTones(Color(0xFF3A4C5B), Color(0xFF97A9B9)),
)
