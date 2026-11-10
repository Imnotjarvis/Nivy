package com.nivy.onboarding

import androidx.compose.runtime.Immutable

@Immutable
enum class OnboardingState {
    SPLASH,
    WELCOME,
    CHOOSE_PATH,
    CURRENCY,
    ACCOUNTS,
    CATEGORIES
}
