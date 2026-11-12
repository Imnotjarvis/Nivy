package com.nivy.design.system.colors

import androidx.compose.ui.graphics.Color

// The palette's third and fourth colors as text: shades that read at 4.5:1, darker on light
// backgrounds than the colors themselves. In minimal colors both are the text color.

/** The color of a payment still to come, for its text and marks on [background]. */
fun upcomingOrange(background: Color): Color = appColors.thirdText.on(background).shown()

/** Tags' color, for their names on [background]. */
fun tagBlue(background: Color): Color = appColors.fourthText.on(background).shown()
