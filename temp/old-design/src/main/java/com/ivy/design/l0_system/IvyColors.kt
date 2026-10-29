package com.ivy.design.l0_system

import androidx.compose.ui.graphics.Color

@Deprecated("Old design system. Use `:ivy-design` and Material3")
interface IvyColors {
    val pure: Color
    val pureInverse: Color

    val gray: Color
    val medium: Color
    val mediumInverse: Color

    val primary: Color

    val green: Color
    val green1: Color

    val orange: Color

    val red: Color

    val isLight: Boolean
}
