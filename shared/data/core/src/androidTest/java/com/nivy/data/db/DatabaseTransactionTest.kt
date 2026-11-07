package com.nivy.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.data.db.entity.AccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTransactionTest {

    private lateinit var db: NivyRoomDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun aBlockCutShortUndoesItsWritesEvenThoseMadeOnTheIoDispatcher() = runBlocking {
        val cash = AccountEntity(name = "Cash", currency = "EUR", color = 0)
        val bank = AccountEntity(name = "Bank", currency = "EUR", color = 0)

        runCatching {
            DatabaseTransaction(db).run {
                db.writeAccountDao.save(cash)
                // As a repository writes: on the IO dispatcher, still in the transaction.
                withContext(Dispatchers.IO) { db.writeAccountDao.save(bank) }
                error("Cut short")
            }
        }

        assertEquals(emptyList<AccountEntity>(), db.accountDao.findAll())
    }
}
