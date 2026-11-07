package com.nivy.domain.usecase.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.db.dao.write.WriteBudgetDao
import com.nivy.data.db.dao.write.WriteLoanDao
import com.nivy.data.db.dao.write.WriteLoanRecordDao
import com.nivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.nivy.data.preferences.LegacySettingsMigration
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Deletes everything the user stored: the database, and the settings in DataStore. */
class DeleteAllDataUseCase @Inject constructor(
    private val dispatchers: DispatchersProvider,
    private val dataObserver: DataObserver,
    private val dataStore: DataStore<Preferences>,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val creditCardRepository: CreditCardRepository,
    private val accountValueRepository: AccountValueRepository,
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val db: NivyRoomDatabase,
    private val writePlannedPaymentRuleDao: WritePlannedPaymentRuleDao,
    private val writeBudgetDao: WriteBudgetDao,
    private val writeLoanDao: WriteLoanDao,
    private val writeLoanRecordDao: WriteLoanRecordDao,
) {
    suspend operator fun invoke() {
        withContext(dispatchers.io) {
            // the repositories also drop what they cache
            accountRepository.deleteAll()
            transactionRepository.deleteAll()
            categoryRepository.deleteAll()
            tagRepository.deleteAll()
            creditCardRepository.deleteAll()
            accountValueRepository.deleteAll()
            exchangeRatesRepository.deleteAll()
            writePlannedPaymentRuleDao.deleteAll()
            writeBudgetDao.deleteAll()
            writeLoanDao.deleteAll()
            writeLoanRecordDao.deleteAll()
            // the old settings table, which the database schema no longer has; its values were
            // copied into DataStore
            db.openHelper.writableDatabase.execSQL("DROP TABLE IF EXISTS settings")
            // also forgets the settings in AppPreferences, but not that they were copied
            dataStore.edit {
                it.clear()
                it[LegacySettingsMigration.Migrated] = true
            }
        }
        dataObserver.post(DataWriteEvent.AllDataChange)
    }
}
