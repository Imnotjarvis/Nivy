package com.ivy.domain.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration105to106_TrnRecurringRules : Migration(105, 106) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val trnRulesTable =
            "CREATE TABLE IF NOT EXISTS `transaction_recurring_rules` (`startDate` INTEGER NOT NULL, `intervalSeconds` INTEGER NOT NULL, `type` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amount` REAL NOT NULL, `categoryId` TEXT, `title` TEXT, `description` TEXT, `id` TEXT NOT NULL, PRIMARY KEY(`id`))"
        db.execSQL(trnRulesTable)

        db.execSQL("ALTER TABLE transactions ADD COLUMN recurringRuleId TEXT")
    }
}
