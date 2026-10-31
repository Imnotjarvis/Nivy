package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration113to114_Multi_Currency : Migration(113, 114) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN currency TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN toAmount REAL")
    }
}
