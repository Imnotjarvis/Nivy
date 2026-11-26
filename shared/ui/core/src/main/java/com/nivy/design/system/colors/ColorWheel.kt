package com.nivy.design.system.colors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Where a color sits on the picker's wheel: its [hue] round the ring, 0 to 360 clockwise from red
 * at the top, and its [tone], 0 the lightest to 1 the darkest.
 */
data class WheelPosition(val hue: Float, val tone: Float)

/**
 * The color at [hue] and [tone]: a hue at a tone, as the palette's own colors are, its light, base
 * and dark at 0.25, 0.5 and 0.75, from almost white at 0 to almost black at 1.
 */
fun wheelColor(hue: Float, tone: Float): Color {
    val (saturation, value) = toneOf(tone.coerceIn(0f, 1f))
    return Color.hsv(hue.mod(FullTurn), saturation, value)
}

/** Where this color sits on the wheel: its hue, and the tone nearest its saturation and brightness. */
fun Color.wheelPosition(): WheelPosition {
    val (hue, saturation, value) = hsv()
    val tone = (0..ToneSteps).map { it / ToneSteps.toFloat() }.minBy { step ->
        val (s, v) = toneOf(step)
        (s - saturation) * (s - saturation) + (v - value) * (v - value)
    }
    return WheelPosition(hue, tone)
}

/**
 * The grey at [share] round the minimal colors' ring: 0 a light grey, 1 a near black, evenly apart
 * in lightness, L* 88 to 12, as the greys the picker offered before.
 */
fun wheelGrey(share: Float): Color {
    val lightness = LightestGrey - (LightestGrey - DarkestGrey) * share.coerceIn(0f, 1f)
    val channel = srgb(luminanceOf(lightness))
    return Color(channel, channel, channel)
}

/** Where this color's grey, as light as it, sits on that ring, 0 to 1. */
fun Color.wheelGreyShare(): Float =
    ((LightestGrey - lightnessOf(luminance())) / (LightestGrey - DarkestGrey)).coerceIn(0f, 1f)

/** The share round the grey ring at [angle]: 0 at the top, 1 at the bottom, the same either way round. */
fun greyShareAt(angle: Float): Float {
    val turned = angle.mod(FullTurn)
    return (if (turned <= HalfTurn) turned else FullTurn - turned) / HalfTurn
}

/** The angle, clockwise from the top, 0 to 360, of a point [fromCenter] of a ring. */
fun ringAngle(fromCenter: Offset): Float =
    Math.toDegrees(atan2(fromCenter.x.toDouble(), -fromCenter.y.toDouble())).toFloat().mod(FullTurn)

/** The color as its code: "#" and its red, green and blue in six hex digits. */
fun Color.code(): String = "#%06X".format(toArgb() and RgbMask)

/** The color of a [code], six hex digits with or without a "#"; null for anything else. */
fun colorOfCode(code: String): Color? {
    val digits = code.trim().removePrefix("#")
    if (digits.length != CodeDigits || digits.any { it.digitToIntOrNull(HexRadix) == null }) return null
    return Color(OpaqueAlpha or digits.toInt(HexRadix))
}

/** The code to show for this color: [typed], as it was typed, while it gives this color; else its own. */
fun Color.codeShown(typed: String): String = if (colorOfCode(typed) == this) typed else code()

// A tone's saturation and brightness, between the anchors': almost white, the palette's light,
// base and dark, then almost black.
private fun toneOf(tone: Float): Pair<Float, Float> {
    val segment = ToneAnchors.zipWithNext().firstOrNull { (_, to) -> tone < to.first }
        ?: return ToneAnchors.last().let { it.second to it.third }
    val (from, to) = segment
    val share = (tone - from.first) / (to.first - from.first)
    return (from.second + (to.second - from.second) * share) to (from.third + (to.third - from.third) * share)
}

// Hue, saturation and value, as Color.hsv takes them.
private fun Color.hsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val delta = max - minOf(red, green, blue)
    val hue = when {
        delta == 0f -> 0f
        max == red -> HueSextant * ((green - blue) / delta).mod(Sextants)
        max == green -> GreenHue + HueSextant * (blue - red) / delta
        else -> BlueHue + HueSextant * (red - green) / delta
    }
    return Triple(hue.mod(FullTurn), if (max == 0f) 0f else delta / max, max)
}

// CIE L* from a relative luminance, and back.
private fun lightnessOf(luminance: Float): Float =
    if (luminance > LabEpsilon) LabScale * cbrt(luminance) - LabOffset else LabKappa * luminance

private fun luminanceOf(lightness: Float): Float =
    if (lightness > LabKappa * LabEpsilon) ((lightness + LabOffset) / LabScale).pow(LabCube) else lightness / LabKappa

// A linear channel in sRGB's gamma.
private fun srgb(linear: Float): Float =
    if (linear <= SrgbKnee) linear * SrgbSlope else SrgbScale * linear.pow(1 / SrgbGamma) - SrgbOffset

// Tone, saturation, value.
private val ToneAnchors = listOf(
    Triple(0f, 0.06f, 1f),
    Triple(0.25f, 0.3f, 1f),
    Triple(0.5f, 0.7f, 1f),
    Triple(0.75f, 0.7f, 0.5f),
    Triple(1f, 0.75f, 0.22f),
)
private const val ToneSteps = 100
private const val FullTurn = 360f
private const val HalfTurn = 180f
private const val HueSextant = 60f
private const val Sextants = 6f
private const val GreenHue = 120f
private const val BlueHue = 240f
private const val LightestGrey = 88f
private const val DarkestGrey = 12f
private const val LabEpsilon = 0.008856f
private const val LabKappa = 903.3f
private const val LabScale = 116f
private const val LabOffset = 16f
private const val LabCube = 3
private const val SrgbKnee = 0.0031308f
private const val SrgbSlope = 12.92f
private const val SrgbScale = 1.055f
private const val SrgbOffset = 0.055f
private const val SrgbGamma = 2.4f
private const val RgbMask = 0xFFFFFF
private const val OpaqueAlpha = -0x1000000
private const val CodeDigits = 6
private const val HexRadix = 16
