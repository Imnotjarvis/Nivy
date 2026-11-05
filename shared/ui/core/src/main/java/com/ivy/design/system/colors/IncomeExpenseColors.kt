package com.ivy.design.system.colors

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

/**
 * The red of what deletes: expenses' red on the theme's surface, so that a theme has one red. On a
 * light theme the darker red also gives a delete button's white icon 5.5:1, where the bright one
 * gave 3.4:1.
 */
@Composable
fun deleteRed(): Color = expenseRed(MaterialTheme.colorScheme.surface)
