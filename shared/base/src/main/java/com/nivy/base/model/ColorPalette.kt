package com.nivy.base.model

import androidx.annotation.Keep

/**
 * The palette the app's own colors come from, chosen in the settings: Nivy's, the phone's
 * wallpaper's (Android 12 and later), one of several colors, or one accent.
 */
@Keep
enum class ColorPalette { NIVY, WALLPAPER, OCEAN, SUNSET, SKY, AMBER, PINK, VIOLET, GRAPHITE }
