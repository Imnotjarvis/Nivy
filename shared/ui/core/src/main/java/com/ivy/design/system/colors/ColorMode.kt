package com.ivy.design.system.colors

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Whether the app is drawn in minimal colors: black, white and greys, but for Home's income and
 * expenses and the warnings, which keep their color ([Palette]). Colors read it while drawing, so
 * switching it redraws the app.
 */
object ColorMode {
    var minimal: Boolean by mutableStateOf(false)

    /** Whether the theme is dark. The theme sets it: minimal colors draw with its text color. */
    var dark: Boolean by mutableStateOf(false)
}

/**
 * This color as drawn. In minimal colors a hue is the theme's text color, black or white, as
 * Material's monochrome scheme draws its primary: one tone at full contrast, not a grey per hue.
 */
fun Color.shown(): Color = if (ColorMode.minimal) ink.copy(alpha = alpha) else this

/** In minimal colors, an item's saved color (an account's, a category's, a loan's): the cards' grey. */
internal val itemGrey: Color get() = if (ColorMode.dark) MediumBlack else MediumWhite

private val ink: Color get() = if (ColorMode.dark) White else Black

/**
 * An account's saved color as drawn: in minimal colors every account is the same grey, the cards'
 * one.
 */
@Composable
@ReadOnlyComposable
fun accountColor(argb: Int): Color =
    if (ColorMode.minimal) MaterialTheme.colorScheme.surfaceVariant else argb.toComposeColor()

/**
 * A category's saved color as drawn: in minimal colors the accounts' grey. The pie chart, whose
 * slices are told apart by shade, gives them greys by size instead.
 */
@Composable
@ReadOnlyComposable
fun categoryColor(argb: Int): Color = accountColor(argb)
