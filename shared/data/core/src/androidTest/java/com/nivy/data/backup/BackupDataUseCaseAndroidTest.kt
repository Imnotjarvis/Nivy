package com.nivy.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.di.KotlinxSerializationModule
import com.nivy.data.DataObserver
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.file.FileSystem
import com.nivy.base.model.Theme
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupDataUseCaseAndroidTest {

    private lateinit var db: NivyRoomDatabase
    private lateinit var useCase: BackupDataUseCase
    private lateinit var appPreferences: AppPreferences
    private val json = KotlinxSerializationModule.provideJson()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
        val appContext = InstrumentationRegistry.getInstrumentation().context
        // A new file each time: DataStore allows only one instance per file. Not in the cache
        // directory, which an import clears.
        appPreferences = AppPreferences(
            PreferenceDataStoreFactory.create {
                File(context.filesDir, "backup-test-${UUID.randomUUID()}.preferences_pb")
            }
        )
        useCase = BackupDataUseCase(
            accountDao = db.accountDao,
            budgetDao = db.budgetDao,
            categoryDao = db.categoryDao,
            loanRecordDao = db.loanRecordDao,
            loanDao = db.loanDao,
            plannedPaymentRuleDao = db.plannedPaymentRuleDao,
            transactionDao = db.transactionDao,
            transactionWriter = db.writeTransactionDao,
            appPreferences = appPreferences,
            accountWriter = db.writeAccountDao,
            categoryWriter = db.writeCategoryDao,
            budgetWriter = db.writeBudgetDao,
            loanWriter = db.writeLoanDao,
            loanRecordWriter = db.writeLoanRecordDao,
            plannedPaymentRuleWriter = db.writePlannedPaymentRuleDao,
            context = appContext,
            json = json,
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = FileSystem(appContext),
            dataObserver = DataObserver(),
            tagsReader = db.tagDao,
            tagAssociationReader = db.tagAssociationDao,
            tagsWriter = db.writeTagDao,
            tagAssociationWriter = db.writeTagAssociationDao,
            creditCardDao = db.creditCardDao,
            creditCardWriter = db.writeCreditCardDao,
            accountValueDao = db.accountValueDao,
            accountValueWriter = db.writeAccountValueDao,
            exchangeRatesDao = db.exchangeRatesDao,
            exchangeRatesWriter = db.writeExchangeRatesDao,
            transaction = DatabaseTransaction(db),
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun backup450_150() = runBlocking {
        backupTestCase("450-150")
    }

    @Test
    fun aNivyBackupComesBackWholeAndSoDoesTheBackupMadeOfIt() = runBlocking {
        // given: a backup written by hand, with every kind of row a Nivy backup carries, in a
        // file as the app writes one (UTF-16)
        val text = InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("backups/nivy-2026-11.json").bufferedReader().use { it.readText() }
        val inFile = json.decodeFromString<NivyCompleteData>(text).tables()
        val file = tempAndroidFile("nivy-backup", ".json").apply { writeText(text, Charsets.UTF_16) }

        // when
        useCase.importBackupFile(file.toUri(), onProgress = {}).error shouldBe null

        // then: every row, field for field
        tables() shouldBe inFile
        shouldHaveTheBackupsSettingsAndBalances()

        // The zip the app makes of it restores the same into an empty wallet.
        val exported = tempAndroidFile("exported", ".zip")
        useCase.exportToFile(exported.toUri())
        closeDb()
        createDb()
        useCase.importBackupFile(exported.toUri(), onProgress = {}).error shouldBe null
        tables() shouldBe inFile
        shouldHaveTheBackupsSettingsAndBalances()
    }

    private suspend fun tables() = NivyCompleteData(
        accounts = db.accountDao.findAll(),
        budgets = db.budgetDao.findAll(),
        categories = db.categoryDao.findAll(),
        loanRecords = db.loanRecordDao.findAll(),
        loans = db.loanDao.findAll(),
        plannedPaymentRules = db.plannedPaymentRuleDao.findAll(),
        transactions = db.transactionDao.findAll(),
        tags = db.tagDao.findAll(),
        tagAssociations = db.tagAssociationDao.findAll(),
        creditCards = db.creditCardDao.findAll(),
        accountValues = db.accountValueDao.findAll(),
        exchangeRates = db.exchangeRatesDao.findAllManuallyOverridden(),
    ).tables()

    private suspend fun shouldHaveTheBackupsSettingsAndBalances() {
        appPreferences.settings() shouldBe
            AppSettings(theme = Theme.DARK, baseCurrency = "EUR", bufferAmount = 150.0, name = "Ana")
        appPreferences.showNotifications() shouldBe false
        appPreferences.hideCurrentBalance() shouldBe true
        appPreferences.transfersAsIncomeExpense() shouldBe true
        // The file says the lock is on: it's never taken from a file.
        appPreferences.isAppLockEnabled() shouldBe false
        NivyBackupBalances.forEach { (account, expected) ->
            db.transactionDao.balance(UUID.fromString(account), Instant.EPOCH, Far) shouldBe expected
        }
    }

    private suspend fun backupTestCase(version: String) {
        importBackupZipTestCase(version)
        importBackupJsonTestCase(version)

        // close and re-open the db to ensure fresh data
        closeDb()
        createDb()
        exportsAndImportsTestCase(version)
    }

    private suspend fun importBackupZipTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.zip")

        // when
        val res = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        res.shouldBeSuccessful()
    }

    private suspend fun importBackupJsonTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.json")

        // when
        val res = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        res.shouldBeSuccessful()
    }

    private suspend fun exportsAndImportsTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.zip")
        // preload data
        useCase.importBackupFile(backupUri, onProgress = {}).shouldBeSuccessful()
        val exportedFileUri = tempAndroidFile("exported", ".zip").toUri()

        // when: the exported file is imported into an empty wallet
        useCase.exportToFile(exportedFileUri)
        closeDb()
        createDb()
        val reImportRes = useCase.importBackupFile(exportedFileUri, onProgress = {})

        // then
        reImportRes.shouldBeSuccessful()
    }

    private fun copyTestResourceToInternalStorage(resPath: String): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assetManager = context.assets
        val inputStream = assetManager.open(resPath)
        val outputFile = tempAndroidFile("temp-backup", resPath.split(".").last())
        outputFile.outputStream().use { fileOut ->
            fileOut.write(inputStream.readBytes())
        }
        return Uri.fromFile(outputFile)
    }

    private fun tempAndroidFile(prefix: String, suffix: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File.createTempFile(prefix, suffix, context.filesDir)
    }

    private fun ImportResult.shouldBeSuccessful() {
        failedRows.shouldBeEmpty()
        categoriesImported shouldBeGreaterThan 0
        accountsImported shouldBeGreaterThan 0
        transactionsImported shouldBeGreaterThan 0
    }
}

/** The rows, each table in a fixed order. Settings are checked on their own. */
private fun NivyCompleteData.tables() = NivyCompleteData(
    accounts = accounts.sortedBy { it.id },
    budgets = budgets.sortedBy { it.id },
    categories = categories.sortedBy { it.id },
    loanRecords = loanRecords.sortedBy { it.id },
    loans = loans.sortedBy { it.id },
    plannedPaymentRules = plannedPaymentRules.sortedBy { it.id },
    transactions = transactions.sortedBy { it.id },
    tags = tags.sortedBy { it.id },
    tagAssociations = tagAssociations.sortedBy { "${it.tagId} ${it.associatedId}" },
    creditCards = creditCards.sortedBy { it.id },
    accountValues = accountValues.sortedBy { it.id },
    exchangeRates = exchangeRates.sortedBy { "${it.baseCurrency} ${it.currency}" },
)

private val Far: Instant = Instant.parse("3000-01-01T00:00:00Z")

/** The balances of backups/nivy-2026-11.json's accounts, worked out by hand (see the unit test). */
private val NivyBackupBalances = mapOf(
    "aaaaaaaa-0000-4000-8000-000000000001" to 340.0, // Bank: 2500 - 60 - 100 - 200 - 1000 - 800
    "aaaaaaaa-0000-4000-8000-000000000002" to -62.5, // Cash: -12.5 - 50
    "aaaaaaaa-0000-4000-8000-000000000003" to 80.0, // Amex: 200 - 120
    "aaaaaaaa-0000-4000-8000-000000000004" to 108.0, // Dollars: the 100 sent arrive as 108
    "aaaaaaaa-0000-4000-8000-000000000005" to 1000.0, // Brokerage
    "aaaaaaaa-0000-4000-8000-000000000006" to 500.0, // Old savings, archived
)
