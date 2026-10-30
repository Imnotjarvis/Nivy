package com.ivy.domain.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration114to115_Category_Account_Icons : Migration(114, 115) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN icon TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT")
    }
}
