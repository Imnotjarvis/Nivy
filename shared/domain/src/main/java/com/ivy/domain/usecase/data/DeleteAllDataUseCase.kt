package com.ivy.domain.usecase.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.read.UserDao
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.db.dao.write.WriteSettingsDao
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
    private val userDao: UserDao,
    private val writeSettingsDao: WriteSettingsDao,
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
            writeSettingsDao.deleteAll()
            writePlannedPaymentRuleDao.deleteAll()
            userDao.deleteAll()
            writeBudgetDao.deleteAll()
            writeLoanDao.deleteAll()
            writeLoanRecordDao.deleteAll()
            // also forgets the settings in AppPreferences
            dataStore.edit { it.clear() }
        }
        dataObserver.post(DataWriteEvent.AllDataChange)
    }
}
