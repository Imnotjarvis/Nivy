package com.nivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Credit cards get a table of their own, so that an account can have several, beside its debit
 * card. A credit card account's settings become its card, which its expenses and planned expenses
 * are paid with, so it works as before; accounts keep no card settings. The card takes the
 * account's id, like the card a backup from before gets when it's restored, so restoring one
 * doesn't add the card again.
 */
class Migration135to136_CreditCards : Migration(135, 136) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `credit_cards` (`accountId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `chargeDay` INTEGER NOT NULL, `statementClosingDay` INTEGER, " +
                "`creditLimit` REAL, `id` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `credit_cards` " +
                "(`accountId`, `name`, `chargeDay`, `statementClosingDay`, `creditLimit`, `id`) " +
                "SELECT `id`, `name`, `chargeDay`, `statementClosingDay`, `creditLimit`, `id` " +
                "FROM `accounts` WHERE `type` = 'CREDIT_CARD' AND `chargeDay` IS NOT NULL"
        )
        listOf("transactions", "planned_payment_rules").forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `creditCardId` TEXT")
            db.execSQL(
                "UPDATE `$table` SET `creditCardId` = `accountId` WHERE `type` = 'EXPENSE' " +
                    "AND `accountId` IN (SELECT `accountId` FROM `credit_cards`)"
            )
        }
        dropCardColumnsFromAccounts(db)
    }

    // SQLite can't drop columns on the Android versions the app supports: the table is copied.
    private fun dropCardColumnsFromAccounts(db: SupportSQLiteDatabase) {
        val columns = "`name`, `currency`, `color`, `icon`, `orderNum`, `includeInBalance`, `type`, " +
            "`archived`, `isSynced`, `isDeleted`, `id`"
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `_new_accounts` (`name` TEXT NOT NULL, `currency` TEXT, " +
                "`color` INTEGER NOT NULL, `icon` TEXT, `orderNum` REAL NOT NULL, " +
                "`includeInBalance` INTEGER NOT NULL, `type` TEXT NOT NULL DEFAULT 'OTHER', " +
                "`archived` INTEGER NOT NULL DEFAULT 0, `isSynced` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, `id` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("INSERT INTO `_new_accounts` ($columns) SELECT $columns FROM `accounts`")
        db.execSQL("DROP TABLE `accounts`")
        db.execSQL("ALTER TABLE `_new_accounts` RENAME TO `accounts`")
    }
}
