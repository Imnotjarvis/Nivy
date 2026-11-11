package com.nivy.design.system.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

// How screens and dialogs come and go, alike: rising a little from below as they fade in, over
// what they cover, which dims; leaving, sinking as they fade out.

/** A fifth of a second, starting quickly and settling gently. */
fun <T> appearTween(): TweenSpec<T> = tween(AppearDurationMillis, easing = FastOutSlowInEasing)

/** How far below its place a screen or dialog starts: a tenth of the screen's height. */
const val AppearRiseFraction = 0.1f

/** How dark what a screen or dialog covers gets: Android's dim behind dialogs. */
const val CoveredDimAlpha = 0.6f

/**
 * How far into its coming a menu's panel is solid, before its content fades in on it: a quarter,
 * about 50 ms. Fading in together, the colors behind showed through both, as a flash.
 */
const val PanelSolidAt = 0.25f

private const val AppearDurationMillis = 200
