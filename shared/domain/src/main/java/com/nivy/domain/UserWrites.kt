package com.nivy.domain

import android.database.sqlite.SQLiteException
import androidx.annotation.StringRes
import com.nivy.base.Toaster
import timber.log.Timber
import javax.inject.Inject

/**
 * Changes the person asked for, written with their failure shown. A database that can't take a
 * write, full or locked, throws; uncaught in a view model's scope, that ends the app. Here it shows
 * a message instead, and the screen stays as it was. Cancellation isn't a database failure and goes
 * on as one.
 */
class UserWrites @Inject constructor(private val toaster: Toaster) {
    /** Runs [write]; true when it went through, false when [failed] was shown instead. */
    suspend fun run(@StringRes failed: Int, write: suspend () -> Unit): Boolean = try {
        write()
        true
    } catch (e: SQLiteException) {
        Timber.e(e)
        toaster.show(failed)
        false
    }
}
