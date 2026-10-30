package com.ivy.design.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.IntSize

/** The size of the app's content in pixels, as the app's theme measures it. */
@Suppress("CompositionLocalAllowlist")
val LocalScreenSize = compositionLocalOf { IntSize.Zero }
