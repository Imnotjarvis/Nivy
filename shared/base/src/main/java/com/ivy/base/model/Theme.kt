package com.ivy.base.model

import androidx.compose.runtime.Immutable

// The theme the user picked in settings. Stored by name, so the names must not change.
@Immutable
enum class Theme {
    LIGHT, DARK, AUTO, AMOLED_DARK
}
