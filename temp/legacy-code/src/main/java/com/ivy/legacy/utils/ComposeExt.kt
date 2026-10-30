package com.ivy.legacy.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import com.ivy.navigation.navigation

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@SuppressLint("ComposableNaming")
@Composable
fun onScreenStart(
    cleanUp: () -> Unit = {},
    start: () -> Unit
) {
    val latestStart by rememberUpdatedState(start)
    val latestCleanup by rememberUpdatedState(cleanUp)
    DisposableEffect(navigation().currentScreen) {
        latestStart()
        onDispose { latestCleanup() }
    }
}
