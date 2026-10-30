package com.ivy.design.system.colors

import androidx.compose.ui.graphics.Color

// The Ivy palette. Each hue has a Light and a Dark variant. Account and category colors are
// stored as ARGB ints picked from these values, so a value must never change.

// Neutrals
val White = Color(0xFFFAFAFA)
val MediumWhite = Color(0xFFEFEEF0)
val Gray = Color(0xFF939199)
val MediumBlack = Color(0xFF2B2C2D)
val Black = Color(0xFF111114)
val TrueBlack = Color(0xFF000000)

// The screen backgrounds behind everything, a shade apart from White and Black so cards and
// modals drawn in those stand out.
val BackgroundLight = Color(0xFFFAFAFC)
val BackgroundDark = Color(0xFF09090A)
val Transparent = Color(0x00000000)

// Hues (Ivy is the brand color; Purple is the same value)
val Ivy = Color(0xFF6B4DFF)
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
val IvyLight = Color(0xFFD5CCFF)
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
val IvyDark = Color(0xFF352680)
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

// A vivid purple used by the loans screen, distinct from Purple (the brand color).
val Violet = Color(0xFFA020F0)
