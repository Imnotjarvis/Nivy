package com.ivy.ui.time

import androidx.compose.runtime.staticCompositionLocalOf
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider

// The app's injected time services, for composables that show dates and times. The app's theme
// provides them from Hilt; previews provide device-time versions.

val LocalTimeConverter = staticCompositionLocalOf<TimeConverter> { error("No LocalTimeConverter") }

val LocalTimeProvider = staticCompositionLocalOf<TimeProvider> { error("No LocalTimeProvider") }

val LocalTimeFormatter = staticCompositionLocalOf<TimeFormatter> { error("No LocalTimeFormatter") }
