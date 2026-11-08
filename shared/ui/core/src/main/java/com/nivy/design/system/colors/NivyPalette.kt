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
 * color in minimal colors (Home's income and expenses, warnings); everything else draws with the
 * hues below, which turn grey in minimal colors (see [ColorMode]).
 */
object Palette {
    // Hues (Nivy is the brand color; Purple is the same value)
    val Nivy = Color(0xFF6B4DFF)
    val Purple = Color(0xFF6B4DFF)
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

// The hues as drawn.
val Nivy: Color get() = Palette.Nivy.shown()
val Purple: Color get() = Palette.Purple.shown()
val Purple1: Color get() = Palette.Purple1.shown()
val Purple2: Color get() = Palette.Purple2.shown()
val Blue: Color get() = Palette.Blue.shown()
val Blue2: Color get() = Palette.Blue2.shown()
val Blue3: Color get() = Palette.Blue3.shown()
val Green: Color get() = Palette.Green.shown()
val Green2: Color get() = Palette.Green2.shown()
val Green3: Color get() = Palette.Green3.shown()
val Green4: Color get() = Palette.Green4.shown()
val Yellow: Color get() = Palette.Yellow.shown()
val Orange: Color get() = Palette.Orange.shown()
val Orange2: Color get() = Palette.Orange2.shown()
val Orange3: Color get() = Palette.Orange3.shown()
val Red: Color get() = Palette.Red.shown()
val Red2: Color get() = Palette.Red2.shown()
val Red3: Color get() = Palette.Red3.shown()
val NivyLight: Color get() = Palette.NivyLight.shown()
val Purple1Light: Color get() = Palette.Purple1Light.shown()
val Purple2Light: Color get() = Palette.Purple2Light.shown()
val BlueLight: Color get() = Palette.BlueLight.shown()
val Blue2Light: Color get() = Palette.Blue2Light.shown()
val Blue3Light: Color get() = Palette.Blue3Light.shown()
val GreenLight: Color get() = Palette.GreenLight.shown()
val Green2Light: Color get() = Palette.Green2Light.shown()
val Green3Light: Color get() = Palette.Green3Light.shown()
val Green4Light: Color get() = Palette.Green4Light.shown()
val YellowLight: Color get() = Palette.YellowLight.shown()
val OrangeLight: Color get() = Palette.OrangeLight.shown()
val Orange2Light: Color get() = Palette.Orange2Light.shown()
val Orange3Light: Color get() = Palette.Orange3Light.shown()
val RedLight: Color get() = Palette.RedLight.shown()
val Red2Light: Color get() = Palette.Red2Light.shown()
val Red3Light: Color get() = Palette.Red3Light.shown()
val NivyDark: Color get() = Palette.NivyDark.shown()
val Purple1Dark: Color get() = Palette.Purple1Dark.shown()
val Purple2Dark: Color get() = Palette.Purple2Dark.shown()
val BlueDark: Color get() = Palette.BlueDark.shown()
val Blue2Dark: Color get() = Palette.Blue2Dark.shown()
val Blue3Dark: Color get() = Palette.Blue3Dark.shown()
val GreenDark: Color get() = Palette.GreenDark.shown()
val Green2Dark: Color get() = Palette.Green2Dark.shown()
val Green3Dark: Color get() = Palette.Green3Dark.shown()
val Green4Dark: Color get() = Palette.Green4Dark.shown()
val YellowDark: Color get() = Palette.YellowDark.shown()
val OrangeDark: Color get() = Palette.OrangeDark.shown()
val Orange2Dark: Color get() = Palette.Orange2Dark.shown()
val Orange3Dark: Color get() = Palette.Orange3Dark.shown()
val RedDark: Color get() = Palette.RedDark.shown()
val Red2Dark: Color get() = Palette.Red2Dark.shown()
val Red3Dark: Color get() = Palette.Red3Dark.shown()
