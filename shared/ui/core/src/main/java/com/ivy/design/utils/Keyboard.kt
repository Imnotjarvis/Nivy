package com.ivy.design.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

@SuppressLint("ComposableNaming")
@Composable
fun hideKeyboard() {
    LocalView.current.hideKeyboard()
}

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

@Composable
fun keyboardVisibleState(): State<Boolean> {
    val rootView = LocalView.current

    val keyboardVisible = remember {
        mutableStateOf(false)
    }

    DisposableEffect(Unit) {
        rootView.addKeyboardListener {
            keyboardVisible.value = it
        }
        onDispose { }
    }

    return keyboardVisible
}

fun View.addKeyboardListener(keyboardCallback: (visible: Boolean) -> Unit) {
    doOnLayout {
        // get init state of keyboard
        var keyboardVisible = isKeyboardOpen(this)

        // callback as soon as the layout is set with whether the keyboard is open or not
        keyboardCallback(keyboardVisible)

        // whenever the layout resizes/changes, callback with the state of the keyboard.
        viewTreeObserver.addOnGlobalLayoutListener {
            val keyboardUpdateCheck = isKeyboardOpen(this)
            // since the observer is hit quite often, only callback when there is a change.
            if (keyboardUpdateCheck != keyboardVisible) {
                keyboardCallback(keyboardUpdateCheck)
                keyboardVisible = keyboardUpdateCheck
            }
        }
    }
}

fun isKeyboardOpen(rootView: View): Boolean {
    return try {
        WindowInsetsCompat.toWindowInsetsCompat(rootView.rootWindowInsets, rootView)
            .isVisible(WindowInsetsCompat.Type.ime())
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
