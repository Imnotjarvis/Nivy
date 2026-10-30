package com.ivy.domain.usecase.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.preferences.LegacySettingsMigration
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.ExchangeRatesRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.TransactionRepository
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
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val db: IvyRoomDatabase,
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
