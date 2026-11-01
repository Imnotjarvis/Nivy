package com.ivy.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.ivy.base.file.unzip
import com.ivy.base.file.zip
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.DatabaseTransaction
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.BudgetDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.read.TagAssociationDao
import com.ivy.data.db.dao.read.TagDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.db.dao.write.WriteAccountDao
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.data.db.dao.write.WriteCategoryDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.db.dao.write.WriteTagAssociationDao
import com.ivy.data.db.dao.write.WriteTagDao
import com.ivy.data.db.dao.write.WriteTransactionDao
import com.ivy.data.file.FileSystem
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.preferences.AppSettings
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
    private val transaction: DatabaseTransaction,
) {
    suspend fun exportToFile(
        zipFileUri: Uri
    ) {
        val jsonString = generateJsonBackup()
        val file = createJsonDataFile(jsonString)
        zip(context = context, zipFileUri, listOf(file))
        clearCacheDir()
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

            val completeData = IvyWalletCompleteData(
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
                tagAssociations = tagAssociations.await()
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

    suspend fun importBackupFile(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): ImportResult = withContext(dispatchersProvider.io) {
        return@withContext try {
            // A zip with the JSON inside, or the JSON itself.
            val jsonString = try {
                extractAndReadBackupZip(backupFileUri, onProgress)
            } catch (e: Exception) {
                fileSystem.read(backupFileUri, Charsets.UTF_16).getOrNull()
            } ?: ""

            importJson(jsonString, onProgress)
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
            clearCacheDir()
            // Whatever happened, what's in memory may not match the database any more.
            dataObserver.post(DataWriteEvent.AllDataChange)
        }
    }

    private suspend fun extractAndReadBackupZip(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): String? {
        val folderName = "backup" + System.currentTimeMillis()
        val cacheFolderPath = File(context.cacheDir, folderName)

        unzip(context, backupFileUri, cacheFolderPath)

        val filesArray = cacheFolderPath.listFiles()

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

        return fileSystem.read(filesList[0].toUri(), Charsets.UTF_16).getOrNull()
    }

    suspend fun importJson(
        jsonString: String,
        onProgress: suspend (Double) -> Unit = {},
    ): ImportResult {
        val modifiedJsonString = accommodateExistingAccountsAndCategories(jsonString)
        // Older versions marked rows as deleted, for cloud sync, instead of deleting them, and
        // their backups still carry those rows. They stay out: nothing should come back to life.
        val completeData = json.decodeFromString<IvyWalletCompleteData>(modifiedJsonString)
            .withoutSoftDeletedRows()

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
    private fun IvyWalletCompleteData.withoutSoftDeletedRows() = copy(
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
        val ivyWalletCompleteData = json.decodeFromString<IvyWalletCompleteData>(jsonString)
        return getReplacementPairs(ivyWalletCompleteData).fold(jsonString) { acc, (backupId, existingId) ->
            acc.replace(backupId.toString(), existingId.toString())
        }
    }

    /** Every write in one transaction: a failure anywhere leaves the wallet as it was. */
    private suspend fun insertDataToDb(
        completeData: IvyWalletCompleteData,
        onProgress: suspend (progressPercent: Double) -> Unit = {}
    ) {
        transaction.run {
            transactionWriter.saveMany(completeData.transactions)
            onProgress(ImportProgress.TRANSACTIONS_SAVED)

            accountWriter.saveMany(completeData.accounts)
            budgetWriter.saveMany(completeData.budgets)
            categoryWriter.saveMany(completeData.categories)
            onProgress(ImportProgress.BUDGETS_AND_CATEGORIES_SAVED)

            loanWriter.saveMany(completeData.loans)
            loanRecordWriter.saveMany(completeData.loanRecords)
            onProgress(ImportProgress.LOANS_SAVED)

            tagsWriter.save(completeData.tags)
            tagAssociationWriter.save(completeData.tagAssociations)
            plannedPaymentRuleWriter.saveMany(completeData.plannedPaymentRules)
            onProgress(ImportProgress.EVERYTHING_SAVED)
        }
    }

    /** The settings a backup carries, applied once its data is in. */
    private suspend fun applySettings(completeData: IvyWalletCompleteData) {
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
     * instead of adding a duplicate. Two same-named rows on the same side are left alone: they
     * are distinct rows, whether in the backup or in the wallet.
     *
     * Returns (id in the backup, id it is replaced with).
     */
    private suspend fun getReplacementPairs(
        completeData: IvyWalletCompleteData
    ): List<Pair<UUID, UUID>> = withContext(dispatchersProvider.io) {
        val existingAccounts = accountDao.findAll().associateBy { it.name }
        val existingCategories = categoryDao.findAll().associateBy { it.name }

        val accounts = completeData.accounts.mapNotNull { backup ->
            existingAccounts[backup.name]?.takeIf { it.id != backup.id }?.let { backup.id to it.id }
        }
        val categories = completeData.categories.mapNotNull { backup ->
            existingCategories[backup.name]?.takeIf { it.id != backup.id }?.let { backup.id to it.id }
        }
        accounts + categories
    }

    private fun hasJsonExtension(file: File): Boolean {
        val name = file.name
        val lastIndexOf = name.lastIndexOf(".")
        if (lastIndexOf == -1) {
            return false
        }

        return (name.substring(lastIndexOf).equals(".json", true))
    }

    private fun clearCacheDir() {
        context.cacheDir.deleteRecursively()
    }
}
