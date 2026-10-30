package com.ivy.design.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat

@Composable
fun windowInsets(): WindowInsetsCompat {
    val rootView = LocalView.current
    return WindowInsetsCompat.toWindowInsetsCompat(rootView.rootWindowInsets, rootView)
}

@Composable
fun navigationBarInsets(): Insets {
    val windowInsets = windowInsets()
    return windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
}

@Composable
fun keyboardOnlyWindowInsets(): Insets {
    val windowInsets = windowInsets()
    return windowInsets.getInsets(
        WindowInsetsCompat.Type.ime()
    )
}

@Composable
fun statusBarInset(): Int {
    val windowInsets = windowInsets()
    return windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
}

@Composable
fun navigationBarInset(): Int {
    return navigationBarInsets().bottom
}
