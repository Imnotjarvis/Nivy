package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Drops the users table, left over from cloud accounts. The settings table moved to DataStore;
 * it stays in the file until LegacySettingsMigration has copied it.
 */
@Suppress("MagicNumber", "ClassNaming")
class Migration130to131_DropUsers : Migration(130, 131) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS users")
    }
}
