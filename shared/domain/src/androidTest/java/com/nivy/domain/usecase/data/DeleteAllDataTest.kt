package com.nivy.domain.usecase.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.IntervalType
import com.nivy.base.model.LoanType
import com.nivy.base.model.TransactionType
import com.nivy.data.DataObserver
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.AccountValueEntity
import com.nivy.data.db.entity.BudgetEntity
import com.nivy.data.db.entity.CategoryEntity
import com.nivy.data.db.entity.CreditCardEntity
import com.nivy.data.db.entity.ExchangeRateEntity
import com.nivy.data.db.entity.LoanEntity
import com.nivy.data.db.entity.LoanRecordEntity
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TagEntity
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.LegacySettingsMigration
import com.nivy.data.remote.RemoteExchangeRatesDataSource
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.fake.fakeRepositoryMemoFactory
import com.nivy.data.repository.mapper.AccountMapper
import com.nivy.data.repository.mapper.AccountValueMapper
import com.nivy.data.repository.mapper.BudgetMapper
import com.nivy.data.repository.mapper.CategoryMapper
import com.nivy.data.repository.mapper.CreditCardMapper
import com.nivy.data.repository.mapper.ExchangeRateMapper
import com.nivy.data.repository.mapper.LoanMapper
import com.nivy.data.repository.mapper.PlannedPaymentRuleMapper
import com.nivy.data.repository.mapper.TagMapper
import com.nivy.data.repository.mapper.TransactionMapper
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.UUID

/** "Delete all data" against a real database: every table it holds ends up empty. */
@RunWith(AndroidJUnit4::class)
class DeleteAllDataTest {
    private lateinit var db: NivyRoomDatabase
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var useCase: DeleteAllDataUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
        // A new file each time: DataStore allows only one instance per file.
        dataStore = PreferenceDataStoreFactory.create {
            File(context.filesDir, "delete-all-test-${UUID.randomUUID()}.preferences_pb")
        }
        val dataObserver = DataObserver()
        val memoFactory = fakeRepositoryMemoFactory(dataObserver)
        val databaseTransaction = DatabaseTransaction(db, dataObserver)
        val dispatchers = TestDispatchersProvider
        val tagRepository = TagRepository(
            mapper = TagMapper(),
            tagDao = db.tagDao,
            tagAssociationDao = db.tagAssociationDao,
            writeTagDao = db.writeTagDao,
            writeTagAssociationDao = db.writeTagAssociationDao,
            dispatchersProvider = dispatchers,
            memoFactory = memoFactory,
            databaseTransaction = databaseTransaction,
        )
        val accountRepository = AccountRepository(
            mapper = AccountMapper(CurrencyRepository(AppPreferences(dataStore), dispatchers)),
            accountDao = db.accountDao,
            writeAccountDao = db.writeAccountDao,
            dispatchersProvider = dispatchers,
            memoFactory = memoFactory,
        )
        useCase = DeleteAllDataUseCase(
            dispatchers = dispatchers,
            dataObserver = dataObserver,
            dataStore = dataStore,
            databaseTransaction = databaseTransaction,
            db = db,
            accountRepository = accountRepository,
            transactionRepository = TransactionRepository(
                mapper = TransactionMapper(accountRepository),
                transactionDao = db.transactionDao,
                writeTransactionDao = db.writeTransactionDao,
                dispatchersProvider = dispatchers,
                tagRepository = tagRepository,
            ),
            categoryRepository = CategoryRepository(
                mapper = CategoryMapper(),
                writeCategoryDao = db.writeCategoryDao,
                categoryDao = db.categoryDao,
                dispatchersProvider = dispatchers,
                memoFactory = memoFactory,
            ),
            tagRepository = tagRepository,
            creditCardRepository = CreditCardRepository(
                creditCardDao = db.creditCardDao,
                writeCreditCardDao = db.writeCreditCardDao,
                mapper = CreditCardMapper(),
                dispatchersProvider = dispatchers,
            ),
            accountValueRepository = AccountValueRepository(
                accountValueDao = db.accountValueDao,
                writeAccountValueDao = db.writeAccountValueDao,
                mapper = AccountValueMapper(),
                dispatchersProvider = dispatchers,
            ),
            exchangeRatesRepository = ExchangeRatesRepository(
                mapper = ExchangeRateMapper(),
                exchangeRatesDao = db.exchangeRatesDao,
                writeExchangeRatesDao = db.writeExchangeRatesDao,
                remoteExchangeRatesDataSource = RemoteExchangeRatesDataSource { error("No downloads here") },
                dispatchers = dispatchers,
            ),
            plannedPaymentRuleRepository = PlannedPaymentRuleRepository(
                plannedPaymentRuleDao = db.plannedPaymentRuleDao,
                writePlannedPaymentRuleDao = db.writePlannedPaymentRuleDao,
                mapper = PlannedPaymentRuleMapper(),
                dispatchersProvider = dispatchers,
            ),
            budgetRepository = BudgetRepository(
                budgetDao = db.budgetDao,
                writeBudgetDao = db.writeBudgetDao,
                mapper = BudgetMapper(),
                dispatchersProvider = dispatchers,
            ),
            loanRepository = LoanRepository(
                loanDao = db.loanDao,
                writeLoanDao = db.writeLoanDao,
                loanRecordDao = db.loanRecordDao,
                writeLoanRecordDao = db.writeLoanRecordDao,
                mapper = LoanMapper(),
                dispatchersProvider = dispatchers,
            ),
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun everyTableEndsUpEmptyAndTheOldSettingsTableGoes() = runBlocking {
        // given: a row in every table, and the old settings table
        fillEveryTable()
        tables().forEach { it.shouldNotBeEmpty() }
        db.openHelper.writableDatabase.execSQL("CREATE TABLE settings (theme TEXT, currency TEXT)")
        db.openHelper.writableDatabase.execSQL("INSERT INTO settings VALUES ('DARK', 'EUR')")
        dataStore.edit { it[stringPreferencesKey("currency")] = "EUR" }

        // when
        useCase()

        // then
        tables().forEach { it.shouldBeEmpty() }
        db.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'settings'")
            .use { it.count shouldBe 0 }
        dataStore.data.first().asMap() shouldBe mapOf(Pair(LegacySettingsMigration.Migrated, true))
    }

    private suspend fun tables(): List<List<Any>> = listOf(
        db.accountDao.findAll(),
        db.categoryDao.findAll(),
        db.transactionDao.findAll(),
        db.budgetDao.findAll(),
        db.loanDao.findAll(),
        db.loanRecordDao.findAll(),
        db.plannedPaymentRuleDao.findAll(),
        db.tagDao.findAll(),
        db.tagAssociationDao.findAll(),
        db.creditCardDao.findAll(),
        db.accountValueDao.findAll(),
        db.exchangeRatesDao.findAll().first(),
    )

    private suspend fun fillEveryTable() {
        val account = AccountEntity(name = "Bank", color = 0)
        db.writeAccountDao.save(account)
        db.writeCategoryDao.save(CategoryEntity(name = "Food", color = 0))
        val transaction = TransactionEntity(accountId = account.id, type = TransactionType.EXPENSE, amount = 12.0)
        db.writeTransactionDao.save(transaction)
        db.writeBudgetDao.save(
            BudgetEntity(
                name = "Groceries",
                amount = 300.0,
                categoryIdsSerialized = null,
                accountIdsSerialized = null,
                orderId = 0.0,
            )
        )
        val loan = LoanEntity(name = "Car", amount = 500.0, type = LoanType.BORROW)
        db.writeLoanDao.save(loan)
        db.writeLoanRecordDao.save(LoanRecordEntity(loanId = loan.id, amount = 100.0, dateTime = Instant.EPOCH))
        db.writePlannedPaymentRuleDao.save(
            PlannedPaymentRuleEntity(
                startDate = Instant.EPOCH,
                intervalN = 1,
                intervalType = IntervalType.MONTH,
                oneTime = false,
                type = TransactionType.EXPENSE,
                accountId = account.id,
            )
        )
        val tag = TagEntity(
            id = UUID.randomUUID(),
            name = "work",
            description = null,
            color = 0,
            icon = null,
            orderNum = 0.0,
            dateTime = Instant.EPOCH,
            isDeleted = false,
            lastSyncedTime = Instant.EPOCH,
        )
        db.writeTagDao.save(tag)
        db.writeTagAssociationDao.save(
            TagAssociationEntity(
                tagId = tag.id,
                associatedId = transaction.id,
                lastSyncedTime = Instant.EPOCH,
                isDeleted = false,
            )
        )
        db.writeCreditCardDao.saveMany(
            listOf(
                CreditCardEntity(
                    accountId = account.id,
                    name = "Visa",
                    chargeDay = 10,
                    statementClosingDay = null,
                    creditLimit = null,
                    id = UUID.randomUUID(),
                )
            )
        )
        db.writeAccountValueDao.save(
            AccountValueEntity(accountId = account.id, time = Instant.EPOCH, value = 1_000.0, id = UUID.randomUUID())
        )
        db.writeExchangeRatesDao.save(ExchangeRateEntity(baseCurrency = "EUR", currency = "USD", rate = 1.08))
    }
}
