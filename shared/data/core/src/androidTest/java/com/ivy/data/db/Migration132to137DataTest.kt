package com.ivy.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ivy.base.model.AccountType
import com.ivy.base.model.IntervalType
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.LoanType
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.BudgetEntity
import com.ivy.data.db.entity.CategoryEntity
import com.ivy.data.db.entity.ExchangeRateEntity
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.db.entity.TagAssociationEntity
import com.ivy.data.db.entity.TagEntity
import com.ivy.data.db.entity.TransactionEntity
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * A wallet from before Nivy's first database change (132), with a row in every table, opened the
 * way the app opens it after an update: Room runs 132 to 137 and checks the schema. Everything
 * then reads back through the app's own DAOs as it was, with the new columns at their defaults,
 * and the balances still add up.
 */
@RunWith(AndroidJUnit4::class)
class Migration132to137DataTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        IvyRoomDatabase::class.java,
        listOf(IvyRoomDatabase.DeleteSEMigration()),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val bank = UUID.randomUUID()
    private val cash = UUID.randomUUID()
    private val dollars = UUID.randomUUID()
    private val food = UUID.randomUUID()
    private val salaryCategory = UUID.randomUUID()
    private val rentRule = UUID.randomUUID()
    private val loan = UUID.randomUUID()
    private val loanRecord = UUID.randomUUID()
    private val tag = UUID.randomUUID()
    private val budget = UUID.randomUUID()

    private val salary = UUID.randomUUID()
    private val lunch = UUID.randomUUID()
    private val transfer = UUID.randomUUID()
    private val paidRent = UUID.randomUUID()
    private val dueRent = UUID.randomUUID()
    private val loanPayment = UUID.randomUUID()

    @Test
    fun aWalletAt132KeepsEveryRowAt137() {
        helper.createDatabase(TestDb, 132).apply {
            insertWallet()
            close()
        }

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            IvyRoomDatabase::class.java,
            TestDb
        ).addMigrations(*IvyRoomDatabase.migrations()).build()

        runBlocking {
            db.accountDao.findAll() shouldContainExactlyInAnyOrder listOf(
                AccountEntity(
                    name = "Bank", currency = "EUR", color = Green, icon = "bank", orderNum = 0.0,
                    includeInBalance = true, type = AccountType.OTHER, archived = false, id = bank,
                ),
                AccountEntity(
                    name = "Cash", currency = null, color = Black, icon = null, orderNum = 1.0,
                    includeInBalance = false, type = AccountType.OTHER, archived = false, id = cash,
                ),
                AccountEntity(
                    name = "Dollars", currency = "USD", color = Blue, icon = "wallet", orderNum = 2.0,
                    includeInBalance = true, type = AccountType.OTHER, archived = false, id = dollars,
                ),
            )
            db.categoryDao.findAll() shouldContainExactlyInAnyOrder listOf(
                CategoryEntity(name = "Food", color = Orange, icon = "groceries", orderNum = 0.0, id = food),
                CategoryEntity(name = "Salary", color = Green, icon = null, orderNum = 1.0, id = salaryCategory),
            )
            db.transactionDao.findAll() shouldContainExactlyInAnyOrder expectedTransactions()
            db.plannedPaymentRuleDao.findAll() shouldContainExactlyInAnyOrder listOf(
                PlannedPaymentRuleEntity(
                    startDate = Nov1Morning, intervalN = 1, intervalType = IntervalType.MONTH,
                    oneTime = false, type = TransactionType.EXPENSE, accountId = bank, amount = 800.0,
                    categoryId = null, title = "Rent", description = "Flat", creditCardId = null,
                    id = rentRule,
                ),
            )
            db.exchangeRatesDao.findAll().first() shouldContainExactlyInAnyOrder listOf(
                ExchangeRateEntity(baseCurrency = "EUR", currency = "USD", rate = 1.08, manualOverride = true),
                ExchangeRateEntity(baseCurrency = "EUR", currency = "BGN", rate = 1.95583, manualOverride = false),
            )
            db.budgetDao.findAll() shouldContainExactlyInAnyOrder listOf(
                BudgetEntity(
                    name = "Food", amount = 300.0, categoryIdsSerialized = "$food",
                    accountIdsSerialized = "$bank,$cash", orderId = 0.0, id = budget,
                ),
            )
            db.loanDao.findAll() shouldContainExactlyInAnyOrder listOf(
                LoanEntity(
                    name = "Car", amount = 1000.0, type = LoanType.BORROW, color = Blue, icon = "car",
                    orderNum = 0.0, accountId = cash, note = "From Ana", dateTime = LoanDate, id = loan,
                ),
            )
            db.loanRecordDao.findAll() shouldContainExactlyInAnyOrder listOf(
                LoanRecordEntity(
                    loanId = loan, amount = 50.0, note = "November", dateTime = Nov5, interest = false,
                    accountId = cash, convertedAmount = null, loanRecordType = LoanRecordType.DECREASE,
                    id = loanRecord,
                ),
            )
            db.tagDao.findAll() shouldContainExactlyInAnyOrder listOf(
                TagEntity(
                    id = tag, name = "Work", description = "Paid by work", color = Orange, icon = null,
                    orderNum = 0.0, dateTime = Oct1, isDeleted = false, lastSyncedTime = Instant.EPOCH,
                ),
            )
            db.tagAssociationDao.findAll() shouldContainExactlyInAnyOrder listOf(
                TagAssociationEntity(
                    tagId = tag, associatedId = lunch, lastSyncedTime = Instant.EPOCH, isDeleted = false,
                ),
            )
            // The tables added since are there, empty: no account was a credit card before 134.
            db.creditCardDao.findAll().shouldBeEmpty()
            db.accountValueDao.findAll().shouldBeEmpty()

            // Salary in, the transfer and the paid rent out; the rent still due counts for nothing.
            db.transactionDao.balance(bank, Instant.EPOCH, Far) shouldBe 1600.0
            db.transactionDao.balance(cash, Instant.EPOCH, Far) shouldBe -62.5
            db.transactionDao.balance(dollars, Instant.EPOCH, Far) shouldBe 108.0
        }
        db.close()
    }

    private fun expectedTransactions() = listOf(
        TransactionEntity(
            accountId = bank,
            type = TransactionType.INCOME,
            amount = 2500.0,
            title = "Salary",
            description = "November, net",
            dateTime = Nov1,
            categoryId = salaryCategory,
            id = salary,
        ),
        TransactionEntity(
            accountId = cash,
            type = TransactionType.EXPENSE,
            amount = 12.5,
            title = "Lunch",
            dateTime = Nov3,
            categoryId = food,
            attachmentUrl = "receipt.jpg",
            id = lunch,
        ),
        TransactionEntity(
            accountId = bank,
            type = TransactionType.TRANSFER,
            amount = 100.0,
            toAccountId = dollars,
            toAmount = 108.0,
            title = "To dollars",
            dateTime = Nov4,
            id = transfer,
        ),
        TransactionEntity(
            accountId = bank,
            type = TransactionType.EXPENSE,
            amount = 800.0,
            title = "Rent",
            dateTime = Nov1Morning,
            recurringRuleId = rentRule,
            paidForDateTime = Nov1Morning,
            id = paidRent,
        ),
        TransactionEntity(
            accountId = bank,
            type = TransactionType.EXPENSE,
            amount = 800.0,
            title = "Rent",
            dueDate = Dec1Morning,
            recurringRuleId = rentRule,
            id = dueRent,
        ),
        TransactionEntity(
            accountId = cash,
            type = TransactionType.EXPENSE,
            amount = 50.0,
            title = "Car",
            dateTime = Nov5,
            loanId = loan,
            loanRecordId = loanRecord,
            id = loanPayment,
        ),
    )

    private fun SupportSQLiteDatabase.insertWallet() {
        insert(
            "accounts (name, currency, color, icon, orderNum, includeInBalance, isSynced, isDeleted, id)",
            arrayOf("Bank", "EUR", Green, "bank", 0.0, 1, 0, 0, "$bank"),
            arrayOf("Cash", null, Black, null, 1.0, 0, 0, 0, "$cash"),
            arrayOf("Dollars", "USD", Blue, "wallet", 2.0, 1, 0, 0, "$dollars"),
        )
        insert(
            "categories (name, color, icon, orderNum, isSynced, isDeleted, id)",
            arrayOf("Food", Orange, "groceries", 0.0, 0, 0, "$food"),
            arrayOf("Salary", Green, null, 1.0, 0, 0, "$salaryCategory"),
        )
        insert(
            "transactions (accountId, type, amount, toAccountId, toAmount, title, description, " +
                "dateTime, categoryId, dueDate, recurringRuleId, paidForDateTime, attachmentUrl, " +
                "loanId, loanRecordId, isSynced, isDeleted, id)",
            arrayOf(
                "$bank", "INCOME", 2500.0, null, null, "Salary", "November, net",
                Nov1.millis, "$salaryCategory", null, null, null, null, null, null, 0, 0, "$salary",
            ),
            arrayOf(
                "$cash", "EXPENSE", 12.5, null, null, "Lunch", null, Nov3.millis, "$food", null,
                null, null, "receipt.jpg", null, null, 0, 0, "$lunch",
            ),
            arrayOf(
                "$bank", "TRANSFER", 100.0, "$dollars", 108.0, "To dollars", null, Nov4.millis,
                null, null, null, null, null, null, null, 0, 0, "$transfer",
            ),
            arrayOf(
                "$bank", "EXPENSE", 800.0, null, null, "Rent", null, Nov1Morning.millis, null, null,
                "$rentRule", Nov1Morning.millis, null, null, null, 0, 0, "$paidRent",
            ),
            arrayOf(
                "$bank", "EXPENSE", 800.0, null, null, "Rent", null, null, null, Dec1Morning.millis,
                "$rentRule", null, null, null, null, 0, 0, "$dueRent",
            ),
            arrayOf(
                "$cash", "EXPENSE", 50.0, null, null, "Car", null, Nov5.millis, null, null, null,
                null, null, "$loan", "$loanRecord", 0, 0, "$loanPayment",
            ),
        )
        insert(
            "planned_payment_rules (startDate, intervalN, intervalType, oneTime, type, accountId, " +
                "amount, categoryId, title, description, isSynced, isDeleted, id)",
            arrayOf(
                Nov1Morning.millis, 1, "MONTH", 0, "EXPENSE", "$bank", 800.0, null, "Rent", "Flat",
                0, 0, "$rentRule",
            ),
        )
        insert(
            "exchange_rates (baseCurrency, currency, rate, manualOverride)",
            arrayOf("EUR", "USD", 1.08, 1),
            arrayOf("EUR", "BGN", 1.95583, 0),
        )
        insert(
            "budgets (name, amount, categoryIdsSerialized, accountIdsSerialized, isSynced, " +
                "isDeleted, orderId, id)",
            arrayOf("Food", 300.0, "$food", "$bank,$cash", 0, 0, 0.0, "$budget"),
        )
        insert(
            "loans (name, amount, type, color, icon, orderNum, accountId, note, isSynced, " +
                "isDeleted, dateTime, id)",
            arrayOf(
                "Car", 1000.0, "BORROW", Blue, "car", 0.0, "$cash", "From Ana", 0, 0,
                LoanDate.toInstant(ZoneOffset.UTC).toEpochMilli(), "$loan",
            ),
        )
        insert(
            "loan_records (loanId, amount, note, dateTime, interest, accountId, convertedAmount, " +
                "loanRecordType, isSynced, isDeleted, id)",
            arrayOf(
                "$loan", 50.0, "November", Nov5.millis, 0, "$cash", null, "DECREASE", 0, 0,
                "$loanRecord",
            ),
        )
        insert(
            "tags (id, name, description, color, icon, orderNum, dateTime, isDeleted, lastSyncedTime)",
            arrayOf("$tag", "Work", "Paid by work", Orange, null, 0.0, Oct1.millis, 0, 0L),
        )
        insert(
            "tags_association (tagId, associatedId, lastSyncedTime, isDeleted)",
            arrayOf("$tag", "$lunch", 0L, 0),
        )
    }

    private fun SupportSQLiteDatabase.insert(into: String, vararg rows: Array<Any?>) {
        val placeholders = rows.first().joinToString { "?" }
        rows.forEach { execSQL("INSERT INTO $into VALUES ($placeholders)", it) }
    }

    private val Instant.millis get() = toEpochMilli()

    private companion object {
        const val TestDb = "migration-132-137-data-test"

        const val Green = -11751600
        const val Black = -16777216
        const val Blue = -14575885
        const val Orange = -26624

        val Oct1: Instant = Instant.parse("2026-10-01T10:00:00Z")
        val Nov1: Instant = Instant.parse("2026-11-01T09:00:00Z")
        val Nov1Morning: Instant = Instant.parse("2026-11-01T08:00:00Z")
        val Nov3: Instant = Instant.parse("2026-11-03T12:30:00Z")
        val Nov4: Instant = Instant.parse("2026-11-04T10:00:00Z")
        val Nov5: Instant = Instant.parse("2026-11-05T18:00:00Z")
        val Dec1Morning: Instant = Instant.parse("2026-12-01T08:00:00Z")
        val LoanDate: LocalDateTime = LocalDateTime.of(2026, 10, 1, 0, 0)
        val Far: Instant = Instant.parse("3000-01-01T00:00:00Z")
    }
}
