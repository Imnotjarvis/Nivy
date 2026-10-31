package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration109to110_PlannedPayments : Migration(109, 110) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE transaction_recurring_rules")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transaction_recurring_rules` (`startDate` INTEGER, `intervalN` INTEGER, `intervalType` TEXT, `oneTime` INTEGER NOT NULL, `type` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amount` REAL NOT NULL, `categoryId` TEXT, `title` TEXT, `description` TEXT, `isSynced` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `id` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}
