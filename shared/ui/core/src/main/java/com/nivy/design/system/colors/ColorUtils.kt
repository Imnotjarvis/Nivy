package com.nivy.design.system.colors

import androidx.annotation.ColorInt
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.pow

// Colors at or below this luminance count as dark.
private const val DarkLuminance = 0.5

// The WCAG contrast ratio adds this to both luminances.
private const val ContrastOffset = 0.05f

// A color whose channels differ by less than this is a grey: also the near-neutral Black (#111114).
private const val GreyTolerance = 0.02f

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

/** True for the light Nivy theme, false for the dark and true-black ones. */
fun ColorScheme.isLight(): Boolean = surface.luminance() > DarkLuminance

/**
 * The color for text and icons on [background]: White or Black, whichever reads better on it,
 * the higher contrast ratio as WCAG measures it. The one rule for content on a fill, in every
 * color mode: on a bright green, black (9:1) rather than white (2:1).
 */
fun readableOn(background: Color): Color {
    val lightness = background.luminance()
    val whiteContrast = (White.luminance() + ContrastOffset) / (lightness + ContrastOffset)
    val blackContrast = (lightness + ContrastOffset) / (Black.luminance() + ContrastOffset)
    return if (whiteContrast >= blackContrast) White else Black
}

/**
 * A component's own [tint] on its [background] fill. White content (a button's text, the +)
 * follows [readableOn], the rule for text on fills: black on a bright green. Other tints stay as
 * designed, unless minimal colors turned the fill grey, as they were chosen for the color it no
 * longer has. On a translucent fill the tint stays: what shows through isn't known here.
 */
fun readableTint(background: Color, tint: Color): Color = when {
    background.alpha < 1f -> tint
    tint == White -> readableOn(background)
    ColorMode.minimal && background.isGrey() -> readableOn(background)
    else -> tint
}

private fun Color.isGrey(): Boolean =
    maxOf(red, green, blue) - minOf(red, green, blue) < GreyTolerance

fun isDarkColor(color: Color): Boolean {
    return isDarkColor(color.toArgb())
}

fun isDarkColor(@ColorInt color: Int): Boolean {
    return ColorUtils.calculateLuminance(color) <= DarkLuminance
}

/**
 * A shade of this color that stands out on it, for secondary text and icons on a fill: lighter
 * where [readableOn] puts white text on it, darker where it puts black, so both lean the same way.
 */
fun Color.dynamicContrast(): Color {
    if (ColorMode.minimal) return readableOn(this)
    val pickedColor = this.toHSVSpec()
    val lightContent = readableOn(this) == White

    return when {
        pickedColor.s >= VividMinSaturation && pickedColor.v >= VividMinValue -> {
            // Primary
            if (lightContent) {
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
            if (lightContent) {
                lighten()
            } else {
                darken()
            }
        }
    }
}

private fun Color.lighten(): Color {
    return this.hsv(
        s = LightenedSaturation,
        v = 1f
    )
}

private fun Color.darken(): Color {
    return this.hsv(
        s = DarkenedSaturation,
        v = DarkenedValue
    )
}

private fun Color.toHSVSpec(): HSVSpec {
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

private fun Color.hsv(
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

/**
 * A saved color (an account's, a category's, a loan's) as drawn: in minimal colors its grey, as
 * light as the color is, so items stay apart as they do in color and a grey picked there shows as
 * itself.
 */
fun Int.toComposeColor() = Color(this).asItemColor()

/** A color picked for an item, as the item is drawn: its grey in minimal colors. */
fun Color.asItemColor(): Color = if (ColorMode.minimal) greyscale() else this

/**
 * The grey as light as this color, its luminance kept: the color in black and white, at the same
 * contrast against anything as the color itself.
 */
fun Color.greyscale(): Color {
    val linear = luminance()
    val value = if (linear <= LinearToSrgbKnee) {
        linear * LinearToSrgbSlope
    } else {
        SrgbScale * linear.pow(1 / SrgbGamma) - SrgbOffset
    }
    return Color(value, value, value, alpha)
}

// sRGB's transfer function, from linear light to the stored value.
private const val LinearToSrgbKnee = 0.0031308f
private const val LinearToSrgbSlope = 12.92f
private const val SrgbScale = 1.055f
private const val SrgbOffset = 0.055f
private const val SrgbGamma = 2.4f
