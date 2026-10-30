package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Deletes the rows marked as deleted since [Migration128to129_DeleteIsDeleted]: planned payments of
 * deleted accounts were still only marked, and so were rows imported from old backups. The app no
 * longer filters on the flag.
 */
@Suppress("MagicNumber", "ClassNaming")
class Migration131to132_DeleteSoftDeleted : Migration(131, 132) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "accounts",
            "budgets",
            "categories",
            "loan_records",
            "loans",
            "planned_payment_rules",
            "tags",
            "tags_association",
            "transactions",
        ).forEach { tableName ->
            db.execSQL("DELETE FROM $tableName WHERE isDeleted = 1")
        }
    }
}
