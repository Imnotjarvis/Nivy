package com.ivy.design.system.colors

import androidx.annotation.ColorInt
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// Colors at or below this luminance count as dark.
private const val DarkLuminance = 0.5

// dynamicContrast() sorts a color by its HSV saturation (s) and value (v): vivid colors, pale
// colors and deep colors, and lightens or darkens it to stand out against itself.
private const val VividMinSaturation = 0.5f
private const val VividMinValue = 0.4f
private const val PaleMaxSaturation = 0.5f
private const val PaleMinValue = 0.8f
private const val DeepMinSaturation = 0.1f
private const val DeepMaxValue = 0.6f
private const val LightenedSaturation = 0.3f
private const val DarkenedSaturation = 0.6f
private const val DarkenedValue = 0.5f
private const val HsvComponents = 3

/** True for the light Ivy theme, false for the dark and true-black ones. */
fun ColorScheme.isLight(): Boolean = surface.luminance() > DarkLuminance

fun Color.asBrush(): Brush {
    return Brush.horizontalGradient(listOf(this, this))
}

fun findContrastTextColor(backgroundColor: Color): Color {
    return if (isDarkColor(backgroundColor.toArgb())) White else Black
}

fun isDarkColor(color: Color): Boolean {
    return isDarkColor(color.toArgb())
}

fun isDarkColor(@ColorInt color: Int): Boolean {
    return ColorUtils.calculateLuminance(color) <= DarkLuminance
}

fun Color.dynamicContrast(): Color {
    val pickedColor = this.toHSVSpec()

    return when {
        pickedColor.s >= VividMinSaturation && pickedColor.v >= VividMinValue -> {
            // Primary
            if (isDarkColor(this)) {
                lighten()
            } else {
                darken()
            }
        }

        pickedColor.s <= PaleMaxSaturation && pickedColor.v >= PaleMinValue -> {
            // Light
            darken()
        }

        pickedColor.s >= DeepMinSaturation && pickedColor.v <= DeepMaxValue -> {
            // Dark
            lighten()
        }

        else -> {
            if (isDarkColor(this)) {
                lighten()
            } else {
                darken()
            }
        }
    }
}

fun Color.lighten(): Color {
    return this.hsv(
        s = LightenedSaturation,
        v = 1f
    )
}

fun Color.darken(): Color {
    return this.hsv(
        s = DarkenedSaturation,
        v = DarkenedValue
    )
}

fun Color.toHSVSpec(): HSVSpec {
    val hsv = FloatArray(HsvComponents)
    val color: Int = this.toArgb()
    android.graphics.Color.colorToHSV(color, hsv)
    return HSVSpec(hsv[0], hsv[1], hsv[2])
}

data class HSVSpec(
    val h: Float,
    val s: Float,
    val v: Float
)

fun Color.hsv(
    h: Float? = null,
    s: Float,
    v: Float
): Color {
    val hsv = FloatArray(HsvComponents)
    val color: Int = this.toArgb()
    android.graphics.Color.colorToHSV(color, hsv)

    if (h != null) {
        hsv[0] = h
    }

    hsv[1] = s
    hsv[2] = v

    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Int.toComposeColor() = Color(this)
