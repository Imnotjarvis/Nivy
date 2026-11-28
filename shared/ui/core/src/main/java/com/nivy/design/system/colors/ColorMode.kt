package com.nivy.design.system.colors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.nivy.base.model.ColorPalette

/**
 * Whether the app is drawn in minimal colors: black, white and greys, but for Home's income and
 * expenses and the warnings, which keep their color ([Palette]). Colors read it while drawing, so
 * switching it redraws the app.
 */
object ColorMode {
    var minimal: Boolean by mutableStateOf(false)

    /** The palette the app's own colors come from (see AppColors), as chosen in the settings. */
    var palette: ColorPalette by mutableStateOf(ColorPalette.NIVY)

    /** The phone's wallpaper's colors, on Android 12 and later: the theme reads them. */
    var wallpaper: AppColors? by mutableStateOf(null)

    /** Whether the theme is dark. The theme sets it: minimal colors draw with its text color. */
    var dark: Boolean by mutableStateOf(false)
}

/** The app's own colors, as the palette chosen gives them. */
internal val appColors: AppColors get() = colorsOf(ColorMode.palette, ColorMode.wallpaper)

/**
 * This color as drawn. In minimal colors a hue is the theme's text color, black or white, as
 * Material's monochrome scheme draws its primary: one tone at full contrast, not a grey per hue.
 */
fun Color.shown(): Color = if (ColorMode.minimal) ink.copy(alpha = alpha) else this

/**
 * In minimal colors, the tone of a chart's part by its rank, for parts that aren't an account or a
 * category (which have their own greys), as a split bar's: three greys, each at 3:1 or more
 * against the cards' grey, darkest to lightest on light themes and the reverse on dark.
 */
fun minimalTone(rank: Int): Color {
    val tones = if (ColorMode.dark) TonesDark else TonesLight
    return tones[rank % tones.size]
}

private val TonesLight = listOf(Black, Color(0xFF4A4A4F), Color(0xFF808080))
private val TonesDark = listOf(White, Color(0xFFBDBDBD), Color(0xFF7A7A7A))

private val ink: Color get() = if (ColorMode.dark) White else Black

/** An account's saved color as drawn: in minimal colors its grey (see [toComposeColor]). */
fun accountColor(argb: Int): Color = argb.toComposeColor()

/** A category's saved color as drawn: in minimal colors its grey, as an account's. */
fun categoryColor(argb: Int): Color = argb.toComposeColor()
