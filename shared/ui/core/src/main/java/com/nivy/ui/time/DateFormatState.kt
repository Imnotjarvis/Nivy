package com.nivy.ui.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nivy.base.model.DateDisplayFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The date format the user chose, kept up to date from the settings by the root view model.
 * Compose state, so that dates already on screen redraw when it changes.
 */
@Singleton
class DateFormatState @Inject constructor() {
    var format by mutableStateOf(DateDisplayFormat.Default)
}
