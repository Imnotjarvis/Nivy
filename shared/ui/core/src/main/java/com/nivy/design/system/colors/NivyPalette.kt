package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color

// The Nivy palette. Each hue has a Light and a Dark variant.

// Neutrals
val White = Color(0xFFFAFAFA)
val MediumWhite = Color(0xFFEFEEF0)

// Secondary text and icons. On the light theme a darker grey, 5:1 on White where the dark theme's
// is 3:1, short of the 4.5:1 text needs; darker still in minimal colors, where greys carry what
// hues told apart.
val Gray: Color get() = when {
    ColorMode.dark -> Color(0xFF939199)
    ColorMode.minimal -> Color(0xFF666666)
    else -> Color(0xFF6B6A70)
}

val MediumBlack = Color(0xFF2B2C2D)
val Black = Color(0xFF111114)
val TrueBlack = Color(0xFF000000)

// The screen backgrounds behind everything, a shade apart from White and Black so cards and
// modals drawn in those stand out.
val BackgroundLight = Color(0xFFFAFAFC)
val BackgroundDark = Color(0xFF09090A)
val Transparent = Color(0x00000000)

/**
 * The hues as they are. Account and category colors are stored as ARGB ints picked from these
 * values, so a value must never change. For what's saved, the color pickers and what keeps its
 * color whatever the palette (Home's income and expenses, warnings); the app's own colors draw
 * with the hues below, which come from the palette chosen and turn grey in minimal colors (see
 * [AppColors] and [ColorMode]).
 */
object Palette {
    // Hues (Nivy is the brand color)
    val Nivy = Color(0xFF6B4DFF)
    val Purple1 = Color(0xFFC34CFF)
    val Purple2 = Color(0xFFFF4CFF)
    val Blue = Color(0xFF4CC3FF)
    val Blue2 = Color(0xFF45E6E6)
    val Blue3 = Color(0xFF457BE6)
    val Green = Color(0xFF14CC9E)
    val Green2 = Color(0xFF45E67B)
    val Green3 = Color(0xFF96E645)
    val Green4 = Color(0xFFC7E62E)
    val Yellow = Color(0xFFFFEE33)
    val Orange = Color(0xFFF29F30)
    val Orange2 = Color(0xFFE67B45)
    val Orange3 = Color(0xFFFFC34C)
    val Red = Color(0xFFFF4060)
    val Red2 = Color(0xFFE62E2E)
    val Red3 = Color(0xFFFF4CA6)

    // Light variants
    val NivyLight = Color(0xFFD5CCFF)
    val Purple1Light = Color(0xFFEECCFF)
    val Purple2Light = Color(0xFFFFBFFF)
    val BlueLight = Color(0xFFB3E6FF)
    val Blue2Light = Color(0xFFB3FFFF)
    val Blue3Light = Color(0xFFCCDDFF)
    val GreenLight = Color(0xFFAAF2E0)
    val Green2Light = Color(0xFF99FFBB)
    val Green3Light = Color(0xFFCCFF99)
    val Green4Light = Color(0xFFEEFF99)
    val YellowLight = Color(0xFFFFF799)
    val OrangeLight = Color(0xFFFFDEB3)
    val Orange2Light = Color(0xFFFFCCB3)
    val Orange3Light = Color(0xFFFFDC99)
    val RedLight = Color(0xFFFFCCD5)
    val Red2Light = Color(0xFFFFB3B3)
    val Red3Light = Color(0xFFFFCCE6)

    // Dark variants
    val NivyDark = Color(0xFF352680)
    val Purple1Dark = Color(0xFF622680)
    val Purple2Dark = Color(0xFF802680)
    val BlueDark = Color(0xFF266280)
    val Blue2Dark = Color(0xFF227373)
    val Blue3Dark = Color(0xFF223D73)
    val GreenDark = Color(0xFF0A664F)
    val Green2Dark = Color(0xFF22733D)
    val Green3Dark = Color(0xFF66804D)
    val Green4Dark = Color(0xFF637317)
    val YellowDark = Color(0xFF807719)
    val OrangeDark = Color(0xFF734B17)
    val Orange2Dark = Color(0xFF66371F)
    val Orange3Dark = Color(0xFF806226)
    val RedDark = Color(0xFF801919)
    val Red2Dark = Color(0xFF802030)
    val Red3Dark = Color(0xFF802653)
}

// The hues as drawn: the palette's, named after Nivy's own.
val Nivy: Color get() = appColors.accent.tone.shown()
val Blue: Color get() = appColors.fourth.tone.shown()
val Green: Color get() = appColors.second.tone.shown()
val Orange: Color get() = appColors.third.tone.shown()
val Orange3: Color get() = Palette.Orange3.shown()
val Red: Color get() = Palette.Red.shown()
val NivyLight: Color get() = appColors.accentLight.shown()
val GreenLight: Color get() = appColors.secondLight.shown()
val OrangeLight: Color get() = appColors.thirdLight.shown()
val RedLight: Color get() = Palette.RedLight.shown()
val Red3Light: Color get() = Palette.Red3Light.shown()
val NivyDark: Color get() = appColors.accentDark.shown()
val Purple1Dark: Color get() = Palette.Purple1Dark.shown()
val Blue2Dark: Color get() = Palette.Blue2Dark.shown()
val GreenDark: Color get() = appColors.secondDark.shown()
val OrangeDark: Color get() = appColors.thirdDark.shown()
