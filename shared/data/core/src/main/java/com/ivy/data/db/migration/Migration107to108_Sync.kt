package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration107to108_Sync : Migration(107, 108) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addSyncColumns("accounts")
        db.addSyncColumns("categories")
        db.addSyncColumns("settings")
        db.addSyncColumns("transactions")
    }

    private fun SupportSQLiteDatabase.addSyncColumns(tableName: String) {
        execSQL("ALTER TABLE $tableName ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
        execSQL("ALTER TABLE $tableName ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}
