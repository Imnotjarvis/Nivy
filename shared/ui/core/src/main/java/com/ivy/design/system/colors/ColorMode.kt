package com.ivy.design.system.colors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance

/**
 * Whether the app is drawn in minimal colors: greys only, but for Home's income and expenses and
 * the warnings, which keep their color ([Palette]). Colors read it while drawing, so switching it
 * redraws the app.
 */
object ColorMode {
    var minimal: Boolean by mutableStateOf(false)
}

/** This color as drawn: in minimal colors, the grey of the same lightness. */
fun Color.shown(): Color = if (ColorMode.minimal) toGrey() else this

/** The grey of the same lightness (relative luminance) and transparency. */
fun Color.toGrey(): Color {
    val lightness = luminance()
    return Color(lightness, lightness, lightness, alpha, ColorSpaces.LinearSrgb)
        .convert(ColorSpaces.Srgb)
}
