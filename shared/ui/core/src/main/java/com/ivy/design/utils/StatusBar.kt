package com.ivy.design.utils

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// While this is in the composition, the status bar shows dark icons and text if darkText is true.
@Composable
fun StatusBarDarkText(darkText: Boolean) {
    val view = LocalView.current
    SideEffect {
        setStatusBarDarkTextCompat(view = view, darkText = darkText)
    }
}

// Dark status-bar icons and text, for light screens.
fun setStatusBarDarkTextCompat(view: View, darkText: Boolean) {
    val window = (view.context as? Activity)?.window ?: return
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkText
}
