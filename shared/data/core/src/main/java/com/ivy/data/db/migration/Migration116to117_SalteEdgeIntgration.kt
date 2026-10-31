package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration116to117_SalteEdgeIntgration : Migration(116, 117) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN seAccountId TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN seCategoryName TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN seTransactionId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN seAutoCategoryId TEXT")
    }
}
