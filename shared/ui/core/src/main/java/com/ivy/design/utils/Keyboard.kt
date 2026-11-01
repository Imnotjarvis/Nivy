package com.ivy.design.utils

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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

// Both read the window's IME inset, which Compose keeps current: the caller recomposes as the
// keyboard opens and closes, with nothing to register or remove.

/** Whether the on-screen keyboard is showing. */
@Composable
fun isKeyboardVisible(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/** The on-screen keyboard's height, 0.dp while it is closed. */
@Composable
fun keyboardHeight(): Dp = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
