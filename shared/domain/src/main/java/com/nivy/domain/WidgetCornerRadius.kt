package com.nivy.domain

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
import com.nivy.base.DefaultCornerRadiusDp
import com.nivy.base.screenCornerRadiusDp
import com.nivy.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The corner radius of the app's blocks, for its widgets on the home screen, which draw without
 * the app open: the radius picked in the settings or, matching the phone, its screen corners'.
 */
class WidgetCornerRadius @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) {
    /** In dp; null before Android 12, where a widget can't round its corners from code. */
    suspend fun dp(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return appPreferences.cornerRadius.first()?.toFloat() ?: screenCornersDp() ?: DefaultCornerRadiusDp
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun screenCornersDp(): Float? = context.getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)
        ?.let { context.screenCornerRadiusDp(it) }
}
