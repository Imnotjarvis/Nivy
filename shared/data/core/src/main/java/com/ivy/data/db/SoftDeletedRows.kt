package com.ivy.data.db

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Older versions marked rows as deleted, for cloud sync, instead of deleting them, and their
 * backups still contain those rows. Nothing reads them, so they are deleted after an import.
 */
object SoftDeletedRows {
    private val Tables = listOf(
        "accounts",
        "budgets",
        "categories",
        "loan_records",
        "loans",
        "planned_payment_rules",
        "tags",
        "tags_association",
        "transactions",
    )

    fun delete(db: SupportSQLiteDatabase) {
        Tables.forEach { tableName ->
            db.execSQL("DELETE FROM $tableName WHERE isDeleted = 1")
        }
    }
}
