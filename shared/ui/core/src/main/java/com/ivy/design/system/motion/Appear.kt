package com.ivy.design.system.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

// How screens and dialogs come and go, alike: rising a little from below as they fade in, over
// what they cover, which dims; leaving, sinking as they fade out.

/** About a third of a second, starting quickly and settling gently. */
fun <T> appearTween(): TweenSpec<T> = tween(AppearDurationMillis, easing = FastOutSlowInEasing)

/** How far below its place a screen or dialog starts: a tenth of the screen's height. */
const val AppearRiseFraction = 0.1f

/** How dark what a screen or dialog covers gets: Android's dim behind dialogs. */
const val CoveredDimAlpha = 0.6f

private const val AppearDurationMillis = 300
