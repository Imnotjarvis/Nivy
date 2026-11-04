package com.ivy.design.system.colors

import androidx.compose.ui.graphics.Color

// Palette's green and red read at 2:1 and 3:1 on light backgrounds, short of the 4.5:1 text
// needs: there, darker shades of the same hues take over, at 4.7:1 on the light cards. On dark
// backgrounds the hues themselves read at 4:1 and more. Both keep their color in minimal colors.
private val GreenOnLight = Color(0xFF0C785D)
private val RedOnLight = Color(0xFFD40024)

/** Income's green, for amounts and marks on [background]. */
fun incomeGreen(background: Color): Color = if (isDarkColor(background)) Palette.Green else GreenOnLight

/** Expenses' red, for amounts and marks on [background]. */
fun expenseRed(background: Color): Color = if (isDarkColor(background)) Palette.Red else RedOnLight
