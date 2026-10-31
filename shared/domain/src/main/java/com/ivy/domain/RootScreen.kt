package com.ivy.domain

import android.net.Uri
import java.time.LocalDate

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

    fun <T> pinWidget(widget: Class<T>)

    /**
     * Shows a date picker on [initialDate] that allows dates from [minDate] to [maxDate];
     * [onPicked] gets the chosen date. Dismissing it picks nothing.
     */
    fun pickDate(
        initialDate: LocalDate?,
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        onPicked: (LocalDate) -> Unit,
    )

    /** Asks the user where to save a new file called [fileName]. */
    fun createNewFile(fileName: String, onCreated: (Uri) -> Unit)

    /** Asks the user to pick a file to open. */
    fun openFile(onOpened: (Uri) -> Unit)
}
