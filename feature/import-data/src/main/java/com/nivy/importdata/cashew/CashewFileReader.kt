package com.nivy.importdata.cashew

import android.database.sqlite.SQLiteDatabase
import java.io.File
import javax.inject.Inject

/** Reads a Cashew data file, a SQLite database, with Android's SQLite. */
internal class CashewFileReader @Inject constructor() {
    /**
     * @throws NotCashewData when it's a database, but not Cashew's.
     * @throws android.database.sqlite.SQLiteException when it isn't a database.
     */
    fun read(file: File): CashewData = SQLiteDatabase.openDatabase(
        file.path,
        null,
        // Only read: no locale table written into the file, as Android does by default.
        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    ).use { readCashew(AndroidCashewTables(it)) }
}
