package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color

// Palette's orange and light blue read at 1.9:1 and 1.2:1 on light cards, short of the 4.5:1 text
// needs: there, darker shades of the same hues take over, at 4.6:1 and 4.8:1 on the light cards.
// On dark backgrounds the hues themselves read at 6.5:1 and more. In minimal colors both are the
// text color, as they were.
private val OrangeOnLight = Color(0xFF9E5A00)
private val BlueOnLight = Color(0xFF1A6E9E)

/** The orange of a payment still to come, for its text and marks on [background]. */
fun upcomingOrange(background: Color): Color =
    (if (isDarkColor(background)) Palette.Orange else OrangeOnLight).shown()

/** Tags' blue, for their names on [background]. */
fun tagBlue(background: Color): Color =
    (if (isDarkColor(background)) Palette.BlueLight else BlueOnLight).shown()
