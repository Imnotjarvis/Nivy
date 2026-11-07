package com.nivy.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.model.AccountType
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.fake.fakeRepositoryMemoFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

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
            DatabaseTransaction(db, DataObserver()).run {
                db.writeAccountDao.save(cash)
                // As a repository writes: on the IO dispatcher, still in the transaction.
                withContext(Dispatchers.IO) { db.writeAccountDao.save(bank) }
                error("Cut short")
            }
        }

        assertEquals(emptyList<AccountEntity>(), db.accountDao.findAll())
    }

    @Test
    fun aBlockCutShortLeavesNoneOfItsWritesInTheRepositoriesMemosEither() = runBlocking {
        val observer = DataObserver()
        // An account repository's memo, which has read the (empty) table once.
        val memo = fakeRepositoryMemoFactory(observer).createMemo<Account, AccountId>(
            getDataWriteSaveEvent = DataWriteEvent::SaveAccounts,
            getDateWriteDeleteEvent = DataWriteEvent::DeleteAccounts,
        )
        memo.findAll(findAllOperation = { emptyList() }, sortMemo = { toList() })
        val cash = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Cash"),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.OTHER,
            archived = false,
            orderNum = 0.0,
        )

        runCatching {
            DatabaseTransaction(db, observer).run {
                memo.save(cash) {
                    db.writeAccountDao.save(AccountEntity(name = "Cash", currency = "EUR", color = 0, id = it.id.value))
                }
                error("Cut short")
            }
        }

        assertEquals(emptyList<AccountEntity>(), db.accountDao.findAll())
        assertEquals(emptyMap<AccountId, Account>(), memo.items)
    }
}
