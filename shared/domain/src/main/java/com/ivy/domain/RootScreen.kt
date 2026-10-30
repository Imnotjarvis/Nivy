package com.ivy.domain

import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime

interface RootScreen {
    /**
     * BuildConfig.VERSION_NAME
     */
    val buildVersionName: String

    /**
     * BuildConfig.VERSION_CODE
     */
    val buildVersionCode: Int

    fun openUrlInBrowser(url: String)

    fun shareCSVFile(fileUri: Uri)

    fun shareZipFile(fileUri: Uri)

    fun openGooglePlayAppPage(appId: String)

    fun <T> pinWidget(widget: Class<T>)

    /** Shows a date picker; [onPicked] gets the chosen date, or the initial one on cancel. */
    fun pickDate(
        initialDate: LocalDate?,
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        onPicked: (LocalDate) -> Unit,
    )

    /** Shows a time picker; [onPicked] gets the chosen time. */
    fun pickTime(initialTime: LocalTime?, onPicked: (LocalTime) -> Unit)

    /** Asks the user where to save a new file called [fileName]. */
    fun createNewFile(fileName: String, onCreated: (Uri) -> Unit)

    /** Asks the user to pick a file to open. */
    fun openFile(onOpened: (Uri) -> Unit)
}
