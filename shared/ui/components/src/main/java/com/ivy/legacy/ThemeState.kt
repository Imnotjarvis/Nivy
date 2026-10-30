package com.ivy.legacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.base.model.Theme
import javax.inject.Inject
import javax.inject.Singleton

/** The theme the user picked; IvyWalletTheme resolves AUTO against the system setting. */
@Singleton
class ThemeState @Inject constructor() {
    var theme: Theme by mutableStateOf(Theme.LIGHT)
        private set

    fun switchTheme(theme: Theme) {
        this.theme = theme
    }
}
