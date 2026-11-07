package com.nivy.data.db.dao.write

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.model.TransactionType
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.CategoryEntity
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.db.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WriteCategoryDaoTest {

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
    fun deletingCategoryKeepsItsTransactionsAndPlannedPaymentsUncategorized() = runBlocking {
        val account = AccountEntity(name = "Cash", currency = "EUR", color = 0)
        val food = CategoryEntity(name = "Food", color = 0)
        val transaction = TransactionEntity(
            accountId = account.id,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            dateTime = Instant.EPOCH,
            categoryId = food.id,
        )
        val rule = PlannedPaymentRuleEntity(
            startDate = Instant.EPOCH,
            intervalN = null,
            intervalType = null,
            oneTime = true,
            type = TransactionType.EXPENSE,
            accountId = account.id,
            categoryId = food.id,
        )
        db.writeAccountDao.save(account)
        db.writeCategoryDao.save(food)
        db.writeTransactionDao.save(transaction)
        db.writePlannedPaymentRuleDao.save(rule)

        db.writeCategoryDao.deleteById(food.id)

        assertNull(db.categoryDao.findById(food.id))
        val kept = db.transactionDao.findById(transaction.id)
        assertEquals(500.0, kept?.amount)
        assertNull(kept?.categoryId)
        assertNull(db.plannedPaymentRuleDao.findById(rule.id)?.categoryId)
    }
}
