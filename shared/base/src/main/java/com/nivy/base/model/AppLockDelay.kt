package com.nivy.base.model

import androidx.annotation.Keep

/**
 * How long the app can be away from the screen, with the lock on, before it asks to be unlocked
 * again, chosen in the settings.
 */
@Keep
enum class AppLockDelay(val millis: Long) {
    IMMEDIATELY(millis = 0),
    ONE_MINUTE(millis = 60_000),
    FIVE_MINUTES(millis = 300_000);

    companion object {
        // The minute the app waited before the setting existed.
        val Default = ONE_MINUTE
    }
}
