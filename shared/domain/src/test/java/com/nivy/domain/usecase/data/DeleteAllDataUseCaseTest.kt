package com.nivy.domain.usecase.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nivy.base.TestDispatchersProvider
import com.nivy.data.DataObserver
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.preferences.LegacySettingsMigration
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DeleteAllDataUseCaseTest {
    @get:Rule
    val folder = TemporaryFolder()

    // The database transaction's block runs as it is; deletes says, for each table's delete in
    // turn, whether it was made within one.
    private var inTransaction = false
    private val deletes = mutableListOf<Boolean>()
    private val databaseTransaction = object : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R {
            inTransaction = true
            return try {
                block()
            } finally {
                inTransaction = false
            }
        }
    }
    private val db = mockk<NivyRoomDatabase>(relaxed = true)
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(produceFile = { File(folder.root, "settings.preferences_pb") })
    }

    private val useCase by lazy {
        DeleteAllDataUseCase(
            dispatchers = TestDispatchersProvider,
            dataObserver = DataObserver(),
            dataStore = dataStore,
            databaseTransaction = databaseTransaction,
            db = db,
            accountRepository = mockk<AccountRepository> { coEvery { deleteAll() } answers { delete() } },
            transactionRepository = mockk<TransactionRepository> { coEvery { deleteAll() } answers { delete() } },
            categoryRepository = mockk<CategoryRepository> { coEvery { deleteAll() } answers { delete() } },
            tagRepository = mockk<TagRepository> { coEvery { deleteAll() } answers { delete() } },
            creditCardRepository = mockk<CreditCardRepository> { coEvery { deleteAll() } answers { delete() } },
            accountValueRepository = mockk<AccountValueRepository> { coEvery { deleteAll() } answers { delete() } },
            exchangeRatesRepository = mockk<ExchangeRatesRepository> { coEvery { deleteAll() } answers { delete() } },
            plannedPaymentRuleRepository = mockk<PlannedPaymentRuleRepository> {
                coEvery { deleteAll() } answers { delete() }
            },
            budgetRepository = mockk<BudgetRepository> { coEvery { deleteAll() } answers { delete() } },
            loanRepository = mockk<LoanRepository> { coEvery { deleteAll() } answers { delete() } },
        )
    }

    private fun delete() {
        deletes += inTransaction
    }

    @Test
    fun `every table is deleted in one database transaction`() = runTest {
        // when
        useCase()

        // then: all ten repositories' tables
        deletes shouldBe List(10) { true }
    }

    @Test
    fun `the old settings table goes too`() = runTest {
        // when
        useCase()

        // then
        verify { db.openHelper.writableDatabase.execSQL("DROP TABLE IF EXISTS settings") }
    }

    @Test
    fun `the settings are forgotten, but not that the old ones were copied`() = runTest {
        // given
        dataStore.edit { it[stringPreferencesKey("currency")] = "EUR" }

        // when
        useCase()

        // then
        dataStore.data.first().asMap() shouldBe mapOf(Pair(LegacySettingsMigration.Migrated, true))
    }
}
