package com.nivy.data.preferences

import com.nivy.base.model.Theme

/** The user's settings: what they set in onboarding and on the settings screen. */
data class AppSettings(
    val theme: Theme,
    // ISO code of the currency balances are shown in
    val baseCurrency: String,
    // the monthly savings goal: Budgets compares it with what each month saved
    val bufferAmount: Double,
    val name: String,
)
