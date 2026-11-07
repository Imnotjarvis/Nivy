package com.nivy.design.utils

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

// Same JVM signature as View.hideKeyboard(), hence the JvmName.
@JvmName("hideKeyboardSafely")
@Suppress("SwallowedException")
fun hideKeyboard(view: View) {
    try {
        view.hideKeyboard()
    } catch (ignore: Exception) {
    }
}

fun View.hideKeyboard() {
    val imm: InputMethodManager =
        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

// These read the window's insets, which Compose keeps current: the caller recomposes as the
// keyboard opens and closes, with nothing to register or remove.

/** Whether the on-screen keyboard is showing. */
@Composable
fun isKeyboardVisible(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/** The on-screen keyboard's height, 0.dp while it is closed. */
@Composable
fun keyboardHeight(): Dp = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }

/**
 * The part of the navigation bar the keyboard leaves uncovered: all of it while the keyboard is
 * closed, none once it's open. The bottom padding of a bar that rises with the keyboard.
 */
@Composable
fun uncoveredNavigationBar(): Dp = with(LocalDensity.current) {
    (WindowInsets.navigationBars.getBottom(this) - WindowInsets.ime.getBottom(this)).coerceAtLeast(0).toDp()
}
