package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color

// The colors offered when picking an account or category color: the saved values, as they are.
val NIVY_COLOR_PICKER_COLORS_FREE = with(Palette) {
    listOf(
        // Primary
        Nivy, Purple1, Purple2, Blue, Blue2, Blue3,
        Green, Green2, Green3, Green4, Yellow,
        Orange, Orange2, Orange3, Red, Red2, Red3,
    )
}

val NIVY_COLOR_PICKER_COLORS_PREMIUM = with(Palette) {
    listOf(
        // Light
        NivyLight, Purple1Light, Purple2Light, BlueLight, Blue2Light, Blue3Light,
        GreenLight, Green2Light, Green3Light, Green4Light, YellowLight,
        OrangeLight, Orange2Light, Orange3Light, RedLight, Red2Light, Red3Light,

        // Dark
        NivyDark, Purple1Dark, Purple2Dark, BlueDark, Blue2Dark, Blue3Dark,
        GreenDark, Green2Dark, Green3Dark, Green4Dark, YellowDark,
        OrangeDark, Orange2Dark, Orange3Dark, RedDark, Red2Dark, Red3Dark,
    )
}

/**
 * The colors the pickers offer in minimal colors: greys from near black to a light grey, evenly
 * apart in lightness (L* 12 to 88). Saved like the hues, so a value must never change.
 */
val NIVY_COLOR_PICKER_GREYS = listOf(
    Color(0xFF1F1F1F),
    Color(0xFF353535),
    Color(0xFF4E4E4E),
    Color(0xFF686868),
    Color(0xFF848484),
    Color(0xFFA0A0A0),
    Color(0xFFBEBEBE),
    Color(0xFFDDDDDD),
)
