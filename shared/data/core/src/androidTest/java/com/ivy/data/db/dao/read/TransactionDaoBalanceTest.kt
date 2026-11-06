package com.ivy.data.db.dao.read

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.model.TransactionType
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * The balance the database sums is the one the transactions add up to when read one by one
 * (TransactionMapper, AccountValueFunctions.balance), what can't be read counting for nothing.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoBalanceTest {
    private lateinit var db: IvyRoomDatabase

    private val bank = AccountEntity(name = "Bank", currency = "EUR", color = 0)
    private val cash = AccountEntity(name = "Cash", currency = "EUR", color = 0)
    private val card = AccountEntity(name = "Card", currency = "EUR", color = 0)
    private val gone = UUID.randomUUID()

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IvyRoomDatabase::class.java).build()
        listOf(bank, cash, card).forEach { db.writeAccountDao.save(it) }
        listOf(
            income(bank.id, 1_000.0, day = 1),
            expense(bank.id, 40.0, day = 2),
            transfer(bank.id, 300.0, to = cash.id, day = 3),
            // Received less: a different currency, or a fee.
            transfer(bank.id, 200.0, to = card.id, toAmount = 180.0, day = 4),
            // An invalid received amount is taken as what was sent.
            transfer(cash.id, 30.0, to = bank.id, toAmount = 0.0, day = 11),
            income(cash.id, 15.0, day = 12),
            // An income with a receiving account counts there too, as it always has.
            income(bank.id, 5.0, day = 13).copy(toAccountId = card.id),
            // What can't be read as a transaction counts for nothing:
            transfer(bank.id, 50.0, to = gone, day = 5),
            transfer(gone, 70.0, to = bank.id, day = 6),
            transfer(bank.id, 25.0, to = bank.id, day = 7),
            expense(bank.id, 0.0, day = 8),
            // Not in the range asked: planned, and dated after its end.
            expense(bank.id, 500.0, day = 9).copy(dateTime = null, dueDate = day(9)),
            expense(bank.id, 60.0, day = 40),
        ).forEach { db.writeTransactionDao.save(it) }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun eachAccountsTransactionsAddUpAsWhenReadOneByOne() = runBlocking {
        // 1,000 - 40 - 300 - 200 + 30 + 5
        assertEquals(495.0, balance(bank.id), Cents)
        // 300 - 30 + 15
        assertEquals(285.0, balance(cash.id), Cents)
        // 180 + 5
        assertEquals(185.0, balance(card.id), Cents)
    }

    @Test
    fun onlyTheTransactionsDatedInTheRangeCount() = runBlocking {
        // The two transfers out, on the 3rd and the 4th.
        assertEquals(-500.0, db.transactionDao.balance(bank.id, day(3), day(4)), Cents)
    }

    @Test
    fun anAccountWithoutTransactionsHasNothing() = runBlocking {
        assertEquals(0.0, balance(UUID.randomUUID()), Cents)
    }

    private suspend fun balance(account: UUID): Double = db.transactionDao.balance(account, day(0), day(31))

    private fun income(account: UUID, amount: Double, day: Long) =
        TransactionEntity(accountId = account, type = TransactionType.INCOME, amount = amount, dateTime = day(day))

    private fun expense(account: UUID, amount: Double, day: Long) =
        TransactionEntity(accountId = account, type = TransactionType.EXPENSE, amount = amount, dateTime = day(day))

    private fun transfer(from: UUID, amount: Double, to: UUID, day: Long, toAmount: Double? = null) =
        TransactionEntity(
            accountId = from,
            type = TransactionType.TRANSFER,
            amount = amount,
            toAccountId = to,
            toAmount = toAmount,
            dateTime = day(day),
        )

    private fun day(day: Long): Instant = Start.plusSeconds(day * SecondsInDay)

    private companion object {
        val Start: Instant = Instant.parse("2026-10-01T10:00:00Z")
        const val SecondsInDay = 86_400L
        const val Cents = 0.005
    }
}
