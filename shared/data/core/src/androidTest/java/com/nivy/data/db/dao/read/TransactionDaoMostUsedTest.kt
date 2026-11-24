package com.nivy.data.db.dao.read

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.model.TransactionType
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.db.dao.MostUsedCategoryCases
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The database's categories used most pass [MostUsedCategoryCases], as FakeTransactionDao's copy does. */
@RunWith(AndroidJUnit4::class)
class TransactionDaoMostUsedTest {
    private lateinit var db: NivyRoomDatabase

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
        MostUsedCategoryCases.transactions.forEach { db.writeTransactionDao.save(it) }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun aTypesCategoriesUsedMostSinceATimeComeTheMostFirstThenTheOneUsedLast() = runBlocking {
        val mostUsed = db.transactionDao.findMostUsedCategoryIds(
            type = TransactionType.EXPENSE,
            since = MostUsedCategoryCases.Since,
            limit = MostUsedCategoryCases.Limit,
        )

        assertEquals(MostUsedCategoryCases.MostUsedExpenses, mostUsed)
    }
}
