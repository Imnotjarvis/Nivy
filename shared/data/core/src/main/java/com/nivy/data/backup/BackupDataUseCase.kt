package com.nivy.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.getOrElse
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.dao.read.AccountValueDao
import com.nivy.data.db.dao.read.BudgetDao
import com.nivy.data.db.dao.read.CategoryDao
import com.nivy.data.db.dao.read.CreditCardDao
import com.nivy.data.db.dao.read.ExchangeRatesDao
import com.nivy.data.db.dao.read.LoanDao
import com.nivy.data.db.dao.read.LoanRecordDao
import com.nivy.data.db.dao.read.PlannedPaymentRuleDao
import com.nivy.data.db.dao.read.TagAssociationDao
import com.nivy.data.db.dao.read.TagDao
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.dao.write.WriteAccountDao
import com.nivy.data.db.dao.write.WriteAccountValueDao
import com.nivy.data.db.dao.write.WriteBudgetDao
import com.nivy.data.db.dao.write.WriteCategoryDao
import com.nivy.data.db.dao.write.WriteCreditCardDao
import com.nivy.data.db.dao.write.WriteExchangeRatesDao
import com.nivy.data.db.dao.write.WriteLoanDao
import com.nivy.data.db.dao.write.WriteLoanRecordDao
import com.nivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.nivy.data.db.dao.write.WriteTagAssociationDao
import com.nivy.data.db.dao.write.WriteTagDao
import com.nivy.data.db.dao.write.WriteTransactionDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.hasCurrency
import com.nivy.data.db.keepTransfersOfDeletedAccounts
import com.nivy.data.file.FileSystem
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

// How far the backup import is (0 to 1) after each of its stages.
private object ImportProgress {
    const val UNZIPPED = 0.05
    const val FILES_FOUND = 0.1
    const val PARSED = 0.4
    const val TRANSACTIONS_SAVED = 0.6
    const val BUDGETS_AND_CATEGORIES_SAVED = 0.7
    const val LOANS_SAVED = 0.8
    const val EVERYTHING_SAVED = 0.9
}

// TODO: Legacy code, needs improvements
class BackupDataUseCase @Inject constructor(
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val loanRecordDao: LoanRecordDao,
    private val loanDao: LoanDao,
    private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
    private val transactionDao: TransactionDao,
    private val transactionWriter: WriteTransactionDao,
    private val appPreferences: AppPreferences,
    private val accountWriter: WriteAccountDao,
    private val categoryWriter: WriteCategoryDao,
    private val budgetWriter: WriteBudgetDao,
    private val loanWriter: WriteLoanDao,
    private val loanRecordWriter: WriteLoanRecordDao,
    private val plannedPaymentRuleWriter: WritePlannedPaymentRuleDao,
    @ApplicationContext
    private val context: Context,
    private val json: Json,
    private val dispatchersProvider: DispatchersProvider,
    private val fileSystem: FileSystem,
    private val dataObserver: DataObserver,
    private val tagsReader: TagDao,
    private val tagAssociationReader: TagAssociationDao,
    private val tagsWriter: WriteTagDao,
    private val tagAssociationWriter: WriteTagAssociationDao,
    private val creditCardDao: CreditCardDao,
    private val creditCardWriter: WriteCreditCardDao,
    private val accountValueDao: AccountValueDao,
    private val accountValueWriter: WriteAccountValueDao,
    private val exchangeRatesDao: ExchangeRatesDao,
    private val exchangeRatesWriter: WriteExchangeRatesDao,
    private val transaction: DatabaseTransaction,
) {
    suspend fun exportToFile(
        zipFileUri: Uri
    ) {
        val jsonString = generateJsonBackup()
        val file = createJsonDataFile(jsonString)
        // Only the backup's own temporary file goes, whether the zip was written or not.
        try {
            fileSystem.zip(zipFileUri, listOf(file))
        } finally {
            file.delete()
        }
    }

    private fun createJsonDataFile(jsonString: String): File {
        val fileNamePrefix = "data"
        val fileNameSuffix = ".json"
        val outputDir = context.cacheDir

        val file = File.createTempFile(fileNamePrefix, fileNameSuffix, outputDir)
        FileOutputStream(file).use { it.write(jsonString.toByteArray(Charsets.UTF_16)) }

        return file
    }

    suspend fun generateJsonBackup(): String {
        return withContext(dispatchersProvider.io) {
            val accounts = async { accountDao.findAll() }
            val budgets = async { budgetDao.findAll() }
            val categories = async { categoryDao.findAll() }
            val loanRecords = async { loanRecordDao.findAll() }
            val loans = async { loanDao.findAll() }
            val plannedPaymentRules =
                async { plannedPaymentRuleDao.findAll() }
            val settings = async {
                listOfNotNull(
                    appPreferences.settings()?.let {
                        SettingsBackup(
                            theme = it.theme,
                            currency = it.baseCurrency,
                            bufferAmount = it.bufferAmount,
                            name = it.name,
                        )
                    }
                )
            }
            val transactions = async { transactionDao.findAll() }
            val sharedPrefs = async { getSharedPrefsData() }
            val tags = async { tagsReader.findAll() }
            val tagAssociations = async { tagAssociationReader.findAll() }
            val creditCards = async { creditCardDao.findAll() }
            val accountValues = async { accountValueDao.findAll() }
            val exchangeRates = async { exchangeRatesDao.findAllManuallyOverridden() }

            val completeData = NivyCompleteData(
                accounts = accounts.await(),
                budgets = budgets.await(),
                categories = categories.await(),
                loanRecords = loanRecords.await(),
                loans = loans.await(),
                plannedPaymentRules = plannedPaymentRules.await(),
                settings = settings.await(),
                transactions = transactions.await(),
                sharedPrefs = sharedPrefs.await(),
                tags = tags.await(),
                tagAssociations = tagAssociations.await(),
                creditCards = creditCards.await(),
                accountValues = accountValues.await(),
                exchangeRates = exchangeRates.await(),
            )

            json.encodeToString(completeData)
        }
    }

    // Backups carry a few settings, as strings, under their old SharedPreferences names.
    private suspend fun getSharedPrefsData(): HashMap<String, String> = hashMapOf(
        AppPreferences.BackupShowNotifications to appPreferences.showNotifications().toString(),
        AppPreferences.BackupAppLockEnabled to appPreferences.isAppLockEnabled().toString(),
        AppPreferences.BackupHideCurrentBalance to appPreferences.hideCurrentBalance().toString(),
        AppPreferences.BackupTransfersAsIncomeExpense to
            appPreferences.transfersAsIncomeExpense().toString(),
    )

    // Whatever fails is the result's error, shown to the user.
    @Suppress("TooGenericExceptionCaught")
    suspend fun importBackupFile(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): ImportResult = withContext(dispatchersProvider.io) {
        val unzipped = File(context.cacheDir, "backup" + System.currentTimeMillis())
        return@withContext try {
            // A zip with the JSON inside, or the JSON itself; a file either refuses or can't be
            // read says why.
            val jsonString = if (fileSystem.isZip(backupFileUri)) {
                extractAndReadBackupZip(backupFileUri, unzipped, onProgress)
            } else {
                fileSystem.read(backupFileUri, Charsets.UTF_16).getOrElse { throw it.e }
            }

            importJson(jsonString, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The database is as it was: the import's writes are one transaction.
            Timber.e(e, "Import failed")
            ImportResult(
                rowsFound = 0,
                transactionsImported = 0,
                accountsImported = 0,
                categoriesImported = 0,
                failedRows = persistentListOf(),
                error = e.message ?: e.javaClass.simpleName,
            )
        } finally {
            // Only what this import unzipped goes.
            unzipped.deleteRecursively()
            // Whatever happened, what's in memory may not match the database any more.
            dataObserver.post(DataWriteEvent.AllDataChange)
        }
    }

    private suspend fun extractAndReadBackupZip(
        backupFileUri: Uri,
        folder: File,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): String {
        fileSystem.unzip(backupFileUri, folder)

        val filesArray = folder.listFiles()

        onProgress(ImportProgress.UNZIPPED)

        if (filesArray == null || filesArray.isEmpty()) {
            error("Couldn't unzip")
        }

        val filesList = filesArray.toList().filter {
            hasJsonExtension(it)
        }

        onProgress(ImportProgress.FILES_FOUND)

        if (filesList.size != 1) {
            error("Didn't unzip exactly one file.")
        }

        return fileSystem.read(filesList[0].toUri(), Charsets.UTF_16).getOrElse { throw it.e }
    }

    suspend fun importJson(
        jsonString: String,
        onProgress: suspend (Double) -> Unit = {},
    ): ImportResult {
        val modifiedJsonString = accommodateExistingAccountsAndCategories(jsonString)
        // Older versions marked rows as deleted, for cloud sync, instead of deleting them, and
        // their backups still carry those rows. They stay out: nothing should come back to life.
        val completeData = json.decodeFromString<NivyCompleteData>(modifiedJsonString)
            .withoutSoftDeletedRows()
            .withLegacyCards(json.decodeFromString<LegacyCardAccounts>(modifiedJsonString).accounts)

        onProgress(ImportProgress.PARSED)
        insertDataToDb(completeData = completeData, onProgress = onProgress)
        applySettings(completeData)
        onProgress(1.0)

        return ImportResult(
            rowsFound = completeData.transactions.size,
            transactionsImported = completeData.transactions.size,
            accountsImported = completeData.accounts.size,
            categoriesImported = completeData.categories.size,
            failedRows = persistentListOf()
        )
    }

    // isDeleted is deprecated so nothing new reads it; this is the one place that has to.
    @Suppress("DEPRECATION")
    private fun NivyCompleteData.withoutSoftDeletedRows() = copy(
        accounts = accounts.filterNot { it.isDeleted },
        budgets = budgets.filterNot { it.isDeleted },
        categories = categories.filterNot { it.isDeleted },
        loanRecords = loanRecords.filterNot { it.isDeleted },
        loans = loans.filterNot { it.isDeleted },
        plannedPaymentRules = plannedPaymentRules.filterNot { it.isDeleted },
        transactions = transactions.filterNot { it.isDeleted },
        tags = tags.filterNot { it.isDeleted },
        tagAssociations = tagAssociations.filterNot { it.isDeleted },
    )

    private suspend fun accommodateExistingAccountsAndCategories(jsonString: String): String {
        val nivyCompleteData = json.decodeFromString<NivyCompleteData>(jsonString)
        return getReplacementPairs(nivyCompleteData).fold(jsonString) { acc, (backupId, existingId) ->
            acc.replace(backupId.toString(), existingId.toString())
        }
    }

    /** Every write in one transaction: a failure anywhere leaves the wallet as it was. */
    private suspend fun insertDataToDb(
        completeData: NivyCompleteData,
        onProgress: suspend (progressPercent: Double) -> Unit = {}
    ) {
        transaction.run {
            transactionWriter.saveMany(completeData.transactions)
            onProgress(ImportProgress.TRANSACTIONS_SAVED)

            // An account the backup holds without a currency takes the backup's base currency, as
            // the app that wrote it showed it, not whatever the base currency is when it's read.
            val currency = completeData.settings.firstOrNull()?.currency ?: appPreferences.baseCurrency()
            accountWriter.saveMany(completeData.accounts.map { it.withCurrency(currency) })
            budgetWriter.saveMany(completeData.budgets)
            categoryWriter.saveMany(completeData.categories)
            onProgress(ImportProgress.BUDGETS_AND_CATEGORIES_SAVED)

            loanWriter.saveMany(completeData.loans)
            loanRecordWriter.saveMany(completeData.loanRecords)
            onProgress(ImportProgress.LOANS_SAVED)

            tagsWriter.save(completeData.tags)
            tagAssociationWriter.save(completeData.tagAssociations)
            // Backups from before a deleted transaction took its tags' links with it still carry them.
            tagAssociationWriter.deleteLinksOfDeletedTransactions()
            plannedPaymentRuleWriter.saveMany(completeData.plannedPaymentRules)
            creditCardWriter.saveMany(completeData.creditCards)
            accountValueWriter.saveMany(completeData.accountValues)
            exchangeRatesWriter.saveMany(completeData.exchangeRates)
            // Backups from before deleting an account kept its transfers still carry the ones
            // left behind: they're kept as the database keeps them.
            keepTransfersOfDeletedAccounts(accountDao, transactionDao, transactionWriter)
            onProgress(ImportProgress.EVERYTHING_SAVED)
        }
    }

    private fun AccountEntity.withCurrency(currency: String?): AccountEntity =
        if (currency == null || hasCurrency()) this else copy(currency = currency)

    /** The settings a backup carries, applied once its data is in. */
    private suspend fun applySettings(completeData: NivyCompleteData) {
        completeData.settings.firstOrNull()?.let {
            appPreferences.saveSettings(
                AppSettings(
                    theme = it.theme,
                    baseCurrency = it.currency,
                    bufferAmount = it.bufferAmount,
                    name = it.name,
                )
            )
        }

        // The display settings the backup carries replace the current ones; a setting the
        // file doesn't mention stays as it is. The app lock is never taken from a file: a
        // backup made by someone else, or one without the key, must not switch it off.
        val backupPrefs = completeData.sharedPrefs
        backupPrefs.flag(AppPreferences.BackupShowNotifications)
            ?.let { appPreferences.setShowNotifications(it) }
        backupPrefs.flag(AppPreferences.BackupHideCurrentBalance)
            ?.let { appPreferences.setHideCurrentBalance(it) }
        backupPrefs.flag(AppPreferences.BackupTransfersAsIncomeExpense)
            ?.let { appPreferences.setTransfersAsIncomeExpense(it) }
    }

    private fun Map<String, String>.flag(key: String): Boolean? = this[key]?.toBooleanStrictOrNull()

    /**
     * Pairs each account and category in the backup with the existing one of the same name, so
     * that the backup's rows, and every transaction pointing at them, land on the existing row
     * instead of adding a duplicate. A name that occurs more than once on either side is left
     * alone: those are distinct rows, whether in the backup or in the wallet. Accounts pair only
     * in the same currency.
     *
     * Returns (id in the backup, id it is replaced with).
     */
    private suspend fun getReplacementPairs(
        completeData: NivyCompleteData
    ): List<Pair<UUID, UUID>> = withContext(dispatchersProvider.io) {
        val existingAccounts = accountDao.findAll().uniqueBy { it.name }
        val existingCategories = categoryDao.findAll().uniqueBy { it.name }

        val accounts = completeData.accounts.uniqueBy { it.name }.values.mapNotNull { backup ->
            existingAccounts[backup.name]
                ?.takeIf { it.id != backup.id && it.currency == backup.currency }
                ?.let { backup.id to it.id }
        }
        val categories = completeData.categories.uniqueBy { it.name }.values.mapNotNull { backup ->
            existingCategories[backup.name]?.takeIf { it.id != backup.id }?.let { backup.id to it.id }
        }
        accounts + categories
    }

    /** The rows whose key no other row shares, by that key. */
    private fun <T> List<T>.uniqueBy(key: (T) -> String): Map<String, T> =
        groupBy(key).filterValues { it.size == 1 }.mapValues { it.value.single() }

    private fun hasJsonExtension(file: File): Boolean {
        val name = file.name
        val lastIndexOf = name.lastIndexOf(".")
        if (lastIndexOf == -1) {
            return false
        }

        return (name.substring(lastIndexOf).equals(".json", true))
    }
}
