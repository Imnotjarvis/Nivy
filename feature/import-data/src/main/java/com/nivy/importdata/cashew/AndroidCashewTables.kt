package com.nivy.importdata.cashew

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/** Cashew's tables in [database], read with Android's SQLite. */
internal class AndroidCashewTables(private val database: SQLiteDatabase) : CashewTables {
    override fun rows(table: String): List<Map<String, Any?>> {
        val exists = database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table),
        ).use { it.moveToFirst() }
        if (!exists) return emptyList()
        return database.rawQuery("SELECT * FROM \"$table\"", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add((0 until cursor.columnCount).associate { cursor.getColumnName(it) to cursor.value(it) })
                }
            }
        }
    }

    // A number, a text or null, as Cashew stores everything the import reads.
    private fun Cursor.value(column: Int): Any? = when (getType(column)) {
        Cursor.FIELD_TYPE_INTEGER -> getLong(column)
        Cursor.FIELD_TYPE_FLOAT -> getDouble(column)
        Cursor.FIELD_TYPE_STRING -> getString(column)
        else -> null
    }
}
