package com.nivy.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nivy.data.db.migration.Migration123to124_LoanIncludeDateTime
import com.nivy.data.db.migration.Migration124to125_LoanEditDateTime
import com.nivy.data.db.migration.Migration126to127_LoanRecordType
import com.nivy.data.db.migration.Migration129to130_LoanIncludeNote
import com.nivy.data.db.migration.Migration130to131_DropUsers
import com.nivy.data.db.migration.Migration131to132_DeleteSoftDeleted
import com.nivy.data.db.migration.Migration135to136_CreditCards
import com.nivy.base.model.LoanType
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NivyRoomDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NivyRoomDatabase::class.java,
        listOf(NivyRoomDatabase.DeleteSEMigration()),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate136to137_AccountValues() {
        helper.createDatabase(TestDb, 136).close()

        // A new table: Room adds it, and checks the schema.
        val newDb = helper.runMigrationsAndValidate(TestDb, 137, true)

        newDb.execSQL(
            "INSERT INTO account_values (accountId, time, value, id) " +
                "VALUES ('${UUID.randomUUID()}', 1761933600000, 8940.0, '${UUID.randomUUID()}')"
        )
        newDb.query("SELECT value FROM account_values").use {
            it.moveToFirst()
            it.getDouble(0) shouldBe 8940.0
        }
        newDb.close()
    }

    @Test
    fun migrate135to136_CreditCards() {
        val card = UUID.randomUUID().toString()
        val bank = UUID.randomUUID().toString()
        helper.createDatabase(TestDb, 135).apply {
            execSQL(
                """
                INSERT INTO accounts (name, color, orderNum, includeInBalance, type, archived,
                creditLimit, chargeDay, statementClosingDay, isSynced, isDeleted, id)
                VALUES ('Visa', 0, 0.0, 1, 'CREDIT_CARD', 0, 1500.0, 15, 25, 0, 0, ?)
                """.trimIndent(),
                arrayOf<Any>(card)
            )
            execSQL(
                """
                INSERT INTO accounts (name, color, orderNum, includeInBalance, type, archived,
                isSynced, isDeleted, id)
                VALUES ('Bank', 0, 1.0, 1, 'BANK', 0, 0, 0, ?)
                """.trimIndent(),
                arrayOf<Any>(bank)
            )
            listOf(
                Triple("Card expense", card, "EXPENSE"),
                Triple("Card refund", card, "INCOME"),
                Triple("Bank expense", bank, "EXPENSE"),
            ).forEach { (title, account, type) ->
                execSQL(
                    """
                    INSERT INTO transactions (accountId, type, amount, title, isSynced, isDeleted, id)
                    VALUES (?, ?, 10.0, ?, 0, 0, ?)
                    """.trimIndent(),
                    arrayOf<Any>(account, type, title, UUID.randomUUID().toString())
                )
            }
            execSQL(
                """
                INSERT INTO planned_payment_rules (oneTime, type, accountId, amount, isSynced, isDeleted, id)
                VALUES (1, 'EXPENSE', ?, 12.0, 0, 0, ?)
                """.trimIndent(),
                arrayOf<Any>(card, UUID.randomUUID().toString())
            )
            close()
        }

        // The accounts table is checked too: it no longer has the card columns.
        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            136,
            true,
            Migration135to136_CreditCards()
        )

        // The card account's settings are its card, under its id.
        newDb.query(
            "SELECT accountId, name, chargeDay, statementClosingDay, creditLimit, id FROM credit_cards"
        ).use {
            it.count shouldBe 1
            it.moveToFirst()
            it.getString(0) shouldBe card
            it.getString(1) shouldBe "Visa"
            it.getInt(2) shouldBe 15
            it.getInt(3) shouldBe 25
            it.getDouble(4) shouldBe 1500.0
            it.getString(5) shouldBe card
        }
        // Its expenses and planned expenses are paid with it; nothing else is.
        newDb.query("SELECT title, creditCardId FROM transactions").use {
            buildMap { while (it.moveToNext()) put(it.getString(0), it.getString(1)) } shouldBe
                mapOf("Card expense" to card, "Card refund" to null, "Bank expense" to null)
        }
        newDb.query("SELECT creditCardId FROM planned_payment_rules").use {
            it.moveToFirst()
            it.getString(0) shouldBe card
        }
        newDb.query("SELECT name FROM accounts ORDER BY orderNum").use {
            buildList { while (it.moveToNext()) add(it.getString(0)) } shouldBe listOf("Visa", "Bank")
        }
        newDb.close()
    }

    @Test
    fun migrate131to132_DeleteSoftDeleted() {
        val accountId = UUID.randomUUID().toString()
        helper.createDatabase(TestDb, 131).apply {
            listOf("Kept" to 0, "Deleted" to 1).forEach { (title, isDeleted) ->
                execSQL(
                    """
                    INSERT INTO planned_payment_rules
                    (oneTime, type, accountId, amount, title, isSynced, isDeleted, id)
                    VALUES (1, 'EXPENSE', ?, 10.0, ?, 0, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(accountId, title, isDeleted, UUID.randomUUID().toString())
                )
                execSQL(
                    """
                    INSERT INTO categories (name, color, orderNum, isSynced, isDeleted, id)
                    VALUES (?, 0, 0.0, 0, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(title, isDeleted, UUID.randomUUID().toString())
                )
            }
            close()
        }

        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            132,
            true,
            Migration131to132_DeleteSoftDeleted()
        )

        newDb.query("SELECT title FROM planned_payment_rules").use {
            it.count shouldBe 1
            it.moveToFirst()
            it.getString(0) shouldBe "Kept"
        }
        newDb.query("SELECT name FROM categories").use {
            it.count shouldBe 1
            it.moveToFirst()
            it.getString(0) shouldBe "Kept"
        }
        newDb.close()
    }

    @Test
    fun migrate130to131_DropUsers() {
        helper.createDatabase(TestDb, 130).close()

        // The settings table stays in the file until LegacySettingsMigration copies it into
        // DataStore, so tables the schema dropped aren't checked.
        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            131,
            false,
            Migration130to131_DropUsers()
        )

        newDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'users'").use {
            it.count shouldBe 0
        }
        newDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'settings'").use {
            it.count shouldBe 1
        }
        newDb.close()
    }

    @Test
    fun migrate129to130_LoanIncludeNote() {
        helper.createDatabase(TestDb, 129).apply {
            val insertSql = """
                INSERT INTO loans (name, amount, type, color, icon, orderNum, accountId, isSynced, isDeleted, dateTime, id) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()

            val preparedStatement = compileStatement(insertSql).apply {
                // Bind values
                bindString(1, "Loan 1")
                bindDouble(2, 123.50)
                bindString(3, LoanType.BORROW.name) // Assuming you store enum as name
                bindLong(4, 13)
                bindString(5, "ic")
                bindDouble(6, 3.14)
                bindString(7, UUID.randomUUID().toString())
                bindLong(8, 1)
                bindLong(9, 0)
                bindString(10, "")
                bindString(11, UUID.randomUUID().toString())
            }
            preparedStatement.executeInsert()
            close()
        }

        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            130,
            true,
            Migration129to130_LoanIncludeNote()
        )

        newDb.query("SELECT * FROM loans").apply {
            moveToFirst() shouldBe true
            getString(0) shouldBe "Loan 1"
            getDouble(1) shouldBe 123.50
            getString(2) shouldBe LoanType.BORROW.name
        }
        newDb.close()
    }

    @Test
    fun migrate123to125_LoanDateTime() {
        // given
        helper.createDatabase(TestDb, 123).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            val insertSql = """
                INSERT INTO loans (name, amount, type, color, icon, orderNum, accountId, isSynced, isDeleted, id) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()

            // Assuming you have an instance of LoanEntity named loanEntity
            val preparedStatement = compileStatement(insertSql).apply {
                // Bind the values from your LoanEntity instance to the prepared statement
                bindString(1, "Loan 1")
                bindDouble(2, 123.50)
                bindString(3, LoanType.BORROW.name) // Assuming you store enum as name
                bindLong(4, 13)
                bindString(5, "ic")
                bindDouble(6, 3.14)
                bindString(7, UUID.randomUUID().toString())
                bindLong(8, 1)
                bindLong(9, 0)
                bindString(10, UUID.randomUUID().toString())
            }
            preparedStatement.executeInsert()
            close()
        }

        // when
        helper.runMigrationsAndValidate(
            TestDb,
            124,
            true,
            Migration123to124_LoanIncludeDateTime()
        )
        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            125,
            true,
            Migration124to125_LoanEditDateTime()
        )

        // then
        newDb.query("SELECT * FROM loans").apply {
            moveToFirst() shouldBe true
            getString(0) shouldBe "Loan 1"
            getDouble(1) shouldBe 123.50
            getString(2) shouldBe LoanType.BORROW.name
        }
        newDb.close()
    }

    @Test
    fun migrate126to127_LoanRecordType() {
        // given
        val loanId = UUID.randomUUID().toString()
        val noteString = "here is your note"
        helper.createDatabase(TestDb, 126).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            val insertSql = """
                INSERT INTO loan_records (loanId, amount, note, dateTime, interest, accountId, convertedAmount, isSynced, isDeleted, id) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()
            // Assuming you have an instance of LoanRecordEntity named loanRecordEntity
            val preparedStatement = compileStatement(insertSql).apply {
                // Bind the values from your LoanRecordEntity instance to the prepared statement
                bindString(1, loanId)
                bindDouble(2, 123.50)
                bindString(3, noteString)
                bindString(4, "this will fail, LocalDateTimeNeeded")
                bindLong(5, 0) // interest
                bindString(6, UUID.randomUUID().toString())
                bindDouble(7, 3.14) // convertedAmount
                bindLong(8, 1)
                bindLong(9, 0)
                bindString(10, UUID.randomUUID().toString())
            }
            preparedStatement.executeInsert()
            close()
        }

        // when
        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            127,
            true,
            Migration126to127_LoanRecordType()
        )

        // then
        newDb.query("SELECT * FROM loan_records").apply {
            moveToFirst() shouldBe true
            getString(0) shouldBe loanId
            getDouble(1) shouldBe 123.50
            getString(2) shouldBe noteString
            getString(10) shouldBe "DECREASE"
        }
        newDb.close()
    }

    @Test
    fun migrateAll() {
        // given:
        // Create the oldest version the migrations start from (docs/database.md).
        helper.createDatabase(TestDb, 106).apply {
            close()
        }

        // then:
        // Open latest version of the database.
        // Room validates and executes all migrations.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NivyRoomDatabase::class.java,
            TestDb
        ).addMigrations(*NivyRoomDatabase.migrations()).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    companion object {
        private const val TestDb = "migration-test"
    }
}
