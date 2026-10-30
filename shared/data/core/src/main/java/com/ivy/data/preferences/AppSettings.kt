package com.ivy.data.preferences

import com.ivy.base.model.Theme

/** The user's settings: what they set in onboarding and on the settings screen. */
data class AppSettings(
    val theme: Theme,
    // ISO code of the currency balances are shown in
    val baseCurrency: String,
    // the amount the user wants to keep aside; the home screen shows what's left above it
    val bufferAmount: Double,
    val name: String,
)
