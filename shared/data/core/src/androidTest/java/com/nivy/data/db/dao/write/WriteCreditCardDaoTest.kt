package com.nivy.data.db.dao.write

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.model.TransactionType
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.CreditCardEntity
import com.nivy.data.db.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WriteCreditCardDaoTest {

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
    fun replacingCardsKeepsWhatTheRemovedOneBoughtAsPlainExpensesAndSavesTheRest() = runBlocking {
        val account = AccountEntity(name = "Bank", currency = "EUR", color = 0)
        val visa = card(account, "Visa")
        val amex = card(account, "Amex")
        val bought = TransactionEntity(
            accountId = account.id,
            type = TransactionType.EXPENSE,
            amount = 30.0,
            dateTime = Instant.parse("2026-11-10T12:00:00Z"),
            purchaseDateTime = Instant.parse("2026-10-20T12:00:00Z"),
            chargeShift = 1,
            creditCardId = visa.id,
        )
        db.writeAccountDao.save(account)
        db.writeCreditCardDao.saveMany(listOf(visa, amex))
        db.writeTransactionDao.save(bought)

        val gold = amex.copy(name = "Amex Gold")
        db.writeCreditCardDao.replaceCards(removed = listOf(visa.id), cards = listOf(gold))

        assertEquals(listOf(gold), db.creditCardDao.findByAccountId(account.id))
        // On the date it was charged, as an expense of the account.
        assertEquals(
            bought.copy(creditCardId = null, purchaseDateTime = null, chargeShift = 0),
            db.transactionDao.findById(bought.id),
        )
    }

    private fun card(account: AccountEntity, name: String) = CreditCardEntity(
        accountId = account.id,
        name = name,
        chargeDay = 10,
        statementClosingDay = null,
        creditLimit = null,
        id = UUID.randomUUID(),
    )
}
