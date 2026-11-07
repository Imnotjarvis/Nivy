package com.nivy.wallet

import android.os.SystemClock
import javax.inject.Inject

/**
 * Time since the phone started, its sleep included. It keeps counting while the app is frozen in
 * the background, and no change of the phone's clock moves it: for how long the app was away.
 */
class ElapsedClock @Inject constructor() {
    fun millis(): Long = SystemClock.elapsedRealtime()
}
