package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration119to120_LoanTransactions : Migration(119, 120) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN loanId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN loanRecordId TEXT")

        db.execSQL("ALTER TABLE loan_records ADD COLUMN interest INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE loan_records ADD COLUMN accountId TEXT")
        db.execSQL("ALTER TABLE loan_records ADD COLUMN convertedAmount REAL")
        db.execSQL("ALTER TABLE loans ADD COLUMN accountId TEXT")
    }
}
