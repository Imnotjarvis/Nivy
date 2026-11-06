package com.ivy.data.db.dao.read

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.dao.BalanceCases
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The database's balance passes [BalanceCases], as FakeTransactionDao's copy of it does. */
@RunWith(AndroidJUnit4::class)
class TransactionDaoBalanceTest {
    private lateinit var db: IvyRoomDatabase

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IvyRoomDatabase::class.java).build()
        BalanceCases.accounts.forEach { db.writeAccountDao.save(it) }
        BalanceCases.transactions.forEach { db.writeTransactionDao.save(it) }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun eachAccountsTransactionsAddUpAsWhenReadOneByOne() = runBlocking {
        BalanceCases.cases.forEach {
            val balance = db.transactionDao.balance(it.account, it.start, it.end)
            assertEquals(it.description, it.balance, balance, BalanceCases.Cents)
        }
    }
}
