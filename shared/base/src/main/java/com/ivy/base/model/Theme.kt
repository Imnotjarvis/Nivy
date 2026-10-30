package com.ivy.base.model

import androidx.compose.runtime.Immutable

// The theme the user picked in settings. Stored by name, so the names must not change.
@Immutable
enum class Theme {
    LIGHT, DARK, AUTO, AMOLED_DARK
}

/** The theme the theme switch moves to from this one. */
fun Theme.next(): Theme = when (this) {
    Theme.LIGHT -> Theme.DARK
    Theme.DARK -> Theme.AMOLED_DARK
    Theme.AMOLED_DARK -> Theme.AUTO
    Theme.AUTO -> Theme.LIGHT
}
