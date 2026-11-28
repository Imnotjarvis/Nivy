package com.nivy.base

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.RoundedCorner
import androidx.annotation.RequiresApi

/** The app's corner radius in dp where the phone doesn't report its screen corners, as before Android 12. */
const val DefaultCornerRadiusDp = 16f

/**
 * The radius of [display]'s rounded screen corners, in dp, which Match my phone gives the app's
 * blocks: Android reports it from version 12, as a circle's. Null where it isn't reported.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun Context.screenCornerRadiusDp(display: Display): Float? {
    val corner = display.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT) ?: return null
    return corner.radius / resources.displayMetrics.density
}
