package com.nivy.data.backup

import android.content.Context
import arrow.core.left
import arrow.core.right
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.di.KotlinxSerializationModule
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.DataObserver
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.fake.FakeAccountDao
import com.nivy.data.db.dao.fake.FakeAccountValueDao
import com.nivy.data.db.dao.fake.FakeBudgetDao
import com.nivy.data.db.dao.fake.FakeCategoryDao
import com.nivy.data.db.dao.fake.FakeCreditCardDao
import com.nivy.data.db.dao.fake.FakeExchangeRatesDao
import com.nivy.data.db.dao.fake.FakeLoanDao
import com.nivy.data.db.dao.fake.FakeLoanRecordDao
import com.nivy.data.db.dao.fake.FakePlannedPaymentDao
import com.nivy.data.db.dao.fake.FakeTagAssociationDao
import com.nivy.data.db.dao.fake.FakeTagDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.AccountValueEntity
import com.nivy.data.db.entity.CreditCardEntity
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.file.FileSystem
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import com.nivy.data.preferences.InMemoryPreferencesDataStore
import com.nivy.data.testResource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Instant
import java.util.UUID

class BackupDataUseCaseTest {
    private val json = KotlinxSerializationModule.provideJson()

    @get:Rule
    val cache = TemporaryFolder()

    // The app's cache directory is the temporary folder.
    private val context = mockk<Context>(relaxed = true) { every { cacheDir } answers { cache.root } }

    /** The fakes have no transactions; the block simply runs. */
    private class FakeDatabaseTransaction : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R = block()
    }

    private fun newBackupDataUseCase(
        accountDao: FakeAccountDao = FakeAccountDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        plannedPaymentDao: FakePlannedPaymentDao = FakePlannedPaymentDao(),
        budgetDao: FakeBudgetDao = FakeBudgetDao(),
        appPreferences: AppPreferences = AppPreferences(InMemoryPreferencesDataStore()),
        loanDao: FakeLoanDao = FakeLoanDao(),
        loanRecordDao: FakeLoanRecordDao = FakeLoanRecordDao(),
        tagDao: FakeTagDao = FakeTagDao(),
        tagAssociationDao: FakeTagAssociationDao = FakeTagAssociationDao(),
        creditCardDao: FakeCreditCardDao = FakeCreditCardDao(),
        accountValueDao: FakeAccountValueDao = FakeAccountValueDao(),
        exchangeRatesDao: FakeExchangeRatesDao = FakeExchangeRatesDao(),
        fileSystem: FileSystem = mockk(relaxed = true),
    ): BackupDataUseCase {
        return BackupDataUseCase(
            accountDao = accountDao,
            accountWriter = accountDao,
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionDao,
            categoryWriter = categoryDao,
            budgetWriter = budgetDao,
            loanWriter = loanDao,
            loanRecordWriter = loanRecordDao,
            plannedPaymentRuleWriter = plannedPaymentDao,

            context = context,
            appPreferences = appPreferences,
            json = json,
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = fileSystem,
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            creditCardDao = creditCardDao,
            creditCardWriter = creditCardDao,
            accountValueDao = accountValueDao,
            accountValueWriter = accountValueDao,
            exchangeRatesDao = exchangeRatesDao,
            exchangeRatesWriter = exchangeRatesDao,
            transaction = FakeDatabaseTransaction(),
        )
    }

    private suspend fun backupTestCase(backupVersion: String) {
        // given
        val originalBackupUseCase = newBackupDataUseCase()
        val backupJsonData = testResource("backups/$backupVersion.json")
            .readText(Charsets.UTF_16)

        // when
        val importedDataRes = originalBackupUseCase.importJson(backupJsonData, onProgress = {})

        // then
        importedDataRes.accountsImported shouldBeGreaterThan 0
        importedDataRes.transactionsImported shouldBeGreaterThan 0
        importedDataRes.categoriesImported shouldBeGreaterThan 0
        importedDataRes.failedRows.size shouldBe 0

        // Also - exporting and re-importing the data should work
        // given
        val exportedJson = originalBackupUseCase.generateJsonBackup()

        // when
        val freshBackupUseCase = newBackupDataUseCase()
        val reImportedDataRes = freshBackupUseCase.importJson(exportedJson, onProgress = {})
        // then
        reImportedDataRes shouldBe importedDataRes

        // Finally, exporting again should yield the same result
        freshBackupUseCase.generateJsonBackup() shouldBe exportedJson
    }

    @Test
    fun `a zip refused for what it holds says why, and isn't read as the JSON`() = runTest {
        // given: an archive with an entry that would land outside its folder
        val fileSystem = mockk<FileSystem>(relaxed = true) {
            every { isZip(any()) } returns true
            every { unzip(any(), any()) } throws IOException(ZipSlip)
            every { read(any(), any()) } returns "not JSON".right()
        }

        // when
        val result = newBackupDataUseCase(fileSystem = fileSystem).importBackupFile(mockk(), onProgress = {})

        // then
        result.error shouldBe ZipSlip
    }

    @Test
    fun `a backup that can't be read says why`() = runTest {
        // given
        val fileSystem = mockk<FileSystem>(relaxed = true) {
            every { isZip(any()) } returns false
            every { read(any(), any()) } returns FileSystem.Failure.FileNotFound(FileNotFoundException(NoFile)).left()
        }

        // when
        val result = newBackupDataUseCase(fileSystem = fileSystem).importBackupFile(mockk(), onProgress = {})

        // then
        result.error shouldBe NoFile
    }

    @Test
    fun `an import deletes the files it unzipped, and nothing else in the cache`() = runTest {
        // given
        cache.newFile("thumbnail.png")
        val fileSystem = mockk<FileSystem>(relaxed = true) {
            every { isZip(any()) } returns true
            every { unzip(any(), any()) } answers {
                secondArg<File>().apply { mkdirs() }.resolve("backup.json").writeText("{}")
            }
            every { read(any(), any()) } returns "{}".right()
        }

        // when
        newBackupDataUseCase(fileSystem = fileSystem).importBackupFile(mockk(), onProgress = {})

        // then
        cache.root.list()!!.toList() shouldBe listOf("thumbnail.png")
    }

    @Test
    fun `an export deletes its own temporary file, and nothing else in the cache`() = runTest {
        // given
        cache.newFile("thumbnail.png")

        // when
        newBackupDataUseCase().exportToFile(mockk())

        // then
        cache.root.list()!!.toList() shouldBe listOf("thumbnail.png")
    }

    @Test
    fun `an export that fails leaves no backup behind in the cache`() = runTest {
        // given
        val fileSystem = mockk<FileSystem>(relaxed = true) {
            every { zip(any(), any()) } throws IOException("No space left on device")
        }

        // when
        shouldThrow<IOException> { newBackupDataUseCase(fileSystem = fileSystem).exportToFile(mockk()) }

        // then
        cache.root.list()!!.toList() shouldBe emptyList()
    }

    @Test
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
    }

    @Test
    fun `a Nivy backup comes back whole, and so does the backup made of it`() = runTest {
        // given: a backup written by hand, with every kind of row a Nivy backup carries
        val file = testResource("backups/nivy-2026-11.json").readText()
        val inFile = json.decodeFromString<NivyCompleteData>(file).tables()

        // when
        val restored = Wallet()
        restored.backup.importJson(file)

        // then: every row, field for field
        restored.tables() shouldBe inFile
        restored.shouldHaveTheBackupsSettingsAndBalances()

        // The backup the app makes of it restores the same.
        val again = Wallet()
        again.backup.importJson(restored.backup.generateJsonBackup())
        again.tables() shouldBe inFile
        again.shouldHaveTheBackupsSettingsAndBalances()
    }

    /** A wallet on fakes: the backup use case, and what the wallet holds. */
    private inner class Wallet {
        private val accounts = FakeAccountDao()
        private val categories = FakeCategoryDao()
        private val transactions = FakeTransactionDao(accounts)
        private val plannedPayments = FakePlannedPaymentDao()
        private val budgets = FakeBudgetDao()
        private val loans = FakeLoanDao()
        private val loanRecords = FakeLoanRecordDao()
        private val tags = FakeTagDao()
        private val tagAssociations = FakeTagAssociationDao()
        private val creditCards = FakeCreditCardDao()
        private val accountValues = FakeAccountValueDao()
        private val exchangeRates = FakeExchangeRatesDao()
        val preferences = AppPreferences(InMemoryPreferencesDataStore())
        val backup = newBackupDataUseCase(
            accountDao = accounts,
            categoryDao = categories,
            transactionDao = transactions,
            plannedPaymentDao = plannedPayments,
            budgetDao = budgets,
            appPreferences = preferences,
            loanDao = loans,
            loanRecordDao = loanRecords,
            tagDao = tags,
            tagAssociationDao = tagAssociations,
            creditCardDao = creditCards,
            accountValueDao = accountValues,
            exchangeRatesDao = exchangeRates,
        )

        suspend fun tables() = NivyCompleteData(
            accounts = accounts.findAll(),
            budgets = budgets.findAll(),
            categories = categories.findAll(),
            loanRecords = loanRecords.findAll(),
            loans = loans.findAll(),
            plannedPaymentRules = plannedPayments.findAll(),
            transactions = transactions.findAll(),
            tags = tags.findAll(),
            tagAssociations = tagAssociations.findAll(),
            creditCards = creditCards.findAll(),
            accountValues = accountValues.findAll(),
            exchangeRates = exchangeRates.findAllManuallyOverridden(),
        ).tables()

        suspend fun balance(account: String) =
            transactions.balance(UUID.fromString(account), Instant.EPOCH, Instant.parse("3000-01-01T00:00:00Z"))
    }

    private suspend fun Wallet.shouldHaveTheBackupsSettingsAndBalances() {
        preferences.settings() shouldBe
            AppSettings(theme = Theme.DARK, baseCurrency = "EUR", bufferAmount = 150.0, name = "Ana")
        preferences.showNotifications() shouldBe false
        preferences.hideCurrentBalance() shouldBe true
        preferences.transfersAsIncomeExpense() shouldBe true
        // The file says the lock is on: it's never taken from a file.
        preferences.isAppLockEnabled() shouldBe false
        NivyBackupBalances.forEach { (account, expected) -> balance(account) shouldBe expected }
    }

    @Test
    fun `importing a backup leaves the app lock alone and keeps settings it doesn't mention`() =
        runTest {
            // given: the lock is on, notifications are off
            val appPreferences = AppPreferences(InMemoryPreferencesDataStore())
            appPreferences.setAppLockEnabled(true)
            appPreferences.setShowNotifications(false)
            val useCase = newBackupDataUseCase(appPreferences = appPreferences)
            // a backup that says the lock is off and doesn't mention notifications
            val backupJson = """{"sharedPrefs": {"lock_app": "false", "hide_current_balance": "true"}}"""

            // when
            useCase.importJson(backupJson, onProgress = {})

            // then
            appPreferences.isAppLockEnabled() shouldBe true
            appPreferences.showNotifications() shouldBe false
            appPreferences.hideCurrentBalance() shouldBe true
        }

    @Test
    fun `two same-named accounts in a backup stay two accounts`() = runTest {
        // given: a wallet with one account, so the backup is matched against existing names
        val accountDao = FakeAccountDao()
        accountDao.save(AccountEntity(name = "Savings", color = 1))
        val useCase = newBackupDataUseCase(accountDao = accountDao)
        val cashEur = AccountEntity(name = "Cash", currency = "EUR", color = 2)
        val cashUsd = AccountEntity(name = "Cash", currency = "USD", color = 3)
        val backup = NivyCompleteData(accounts = listOf(cashEur, cashUsd))

        // when
        useCase.importJson(json.encodeToString(backup))

        // then: both arrive under their own ids
        accountDao.findAll().map { it.id } shouldContainExactlyInAnyOrder
            listOf(accountDao.findAll().first { it.name == "Savings" }.id, cashEur.id, cashUsd.id)
    }

    @Test
    fun `a backup account named like an existing one lands on the existing one`() = runTest {
        // given
        val accountDao = FakeAccountDao()
        val transactionDao = FakeTransactionDao()
        val existingCash = AccountEntity(name = "Cash", color = 1)
        accountDao.save(existingCash)
        val useCase = newBackupDataUseCase(accountDao = accountDao, transactionDao = transactionDao)
        val backupCash = AccountEntity(name = "Cash", color = 2)
        val backup = NivyCompleteData(
            accounts = listOf(backupCash),
            transactions = listOf(
                TransactionEntity(accountId = backupCash.id, type = TransactionType.EXPENSE, amount = 5.0)
            ),
        )

        // when
        useCase.importJson(json.encodeToString(backup))

        // then: one Cash, and the imported transaction points at it
        accountDao.findAll().map { it.id } shouldBe listOf(existingCash.id)
        transactionDao.findAll().map { it.accountId } shouldBe listOf(existingCash.id)
    }

    @Test
    fun `re-importing two same-named accounts keeps each transaction on its own account`() =
        runTest {
            // given: the wallet already holds both accounts, e.g. from importing this backup before
            val accountDao = FakeAccountDao()
            val transactionDao = FakeTransactionDao()
            val cashEur = AccountEntity(name = "Cash", currency = "EUR", color = 1)
            val cashUsd = AccountEntity(name = "Cash", currency = "USD", color = 2)
            accountDao.save(cashEur)
            accountDao.save(cashUsd)
            val useCase = newBackupDataUseCase(accountDao = accountDao, transactionDao = transactionDao)
            val backup = NivyCompleteData(
                accounts = listOf(cashEur, cashUsd),
                transactions = listOf(
                    TransactionEntity(accountId = cashEur.id, type = TransactionType.EXPENSE, amount = 5.0)
                ),
            )

            // when
            useCase.importJson(json.encodeToString(backup))

            // then
            transactionDao.findAll().map { it.accountId } shouldBe listOf(cashEur.id)
        }

    @Test
    fun `a same-named backup account in another currency stays a separate account`() = runTest {
        // given
        val accountDao = FakeAccountDao()
        val existingCash = AccountEntity(name = "Cash", currency = "EUR", color = 1)
        accountDao.save(existingCash)
        val useCase = newBackupDataUseCase(accountDao = accountDao)
        val backupCash = AccountEntity(name = "Cash", currency = "USD", color = 2)

        // when
        useCase.importJson(json.encodeToString(NivyCompleteData(accounts = listOf(backupCash))))

        // then: the existing account keeps its currency, and the backup's arrives beside it
        accountDao.findAll().associate { it.id to it.currency } shouldBe
            mapOf(existingCash.id to "EUR", backupCash.id to "USD")
    }

    @Test
    fun `a backup keeps the values entered for investment accounts`() = runTest {
        // given
        val accountDao = FakeAccountDao()
        val accountValueDao = FakeAccountValueDao()
        val portfolio = AccountEntity(name = "Portfolio", color = 1)
        val value = AccountValueEntity(
            accountId = portfolio.id,
            time = Instant.parse("2026-10-31T18:00:00Z"),
            value = 330.0,
            cash = 700.0,
            id = UUID.randomUUID(),
        )
        accountDao.save(portfolio)
        accountValueDao.save(value)
        val backup = newBackupDataUseCase(accountDao, accountValueDao = accountValueDao).generateJsonBackup()

        // when
        val restoredValues = FakeAccountValueDao()
        newBackupDataUseCase(accountValueDao = restoredValues).importJson(backup)

        // then
        restoredValues.findAll() shouldBe listOf(value)
    }

    @Test
    fun `a backup keeps the credit cards and what's paid with them`() = runTest {
        // given: a bank account with a card, and an expense paid with it
        val accountDao = FakeAccountDao()
        val transactionDao = FakeTransactionDao()
        val creditCardDao = FakeCreditCardDao()
        val bank = AccountEntity(name = "Bank", color = 1)
        val visa = CreditCardEntity(
            accountId = bank.id,
            name = "Visa",
            chargeDay = 15,
            statementClosingDay = 25,
            creditLimit = 1500.0,
            id = UUID.randomUUID(),
        )
        accountDao.save(bank)
        creditCardDao.saveMany(listOf(visa))
        transactionDao.save(
            TransactionEntity(accountId = bank.id, type = TransactionType.EXPENSE, amount = 5.0, creditCardId = visa.id)
        )
        val backup = newBackupDataUseCase(accountDao, transactionDao = transactionDao, creditCardDao = creditCardDao)
            .generateJsonBackup()

        // when
        val restoredTransactions = FakeTransactionDao()
        val restoredCards = FakeCreditCardDao()
        newBackupDataUseCase(transactionDao = restoredTransactions, creditCardDao = restoredCards)
            .importJson(backup)

        // then
        restoredCards.findAll() shouldBe listOf(visa)
        restoredTransactions.findAll().map { it.creditCardId } shouldBe listOf(visa.id)
    }

    @Test
    fun `a backup from before cards had their own table brings back its card accounts' cards`() = runTest {
        // given: a credit card account with its card settings, as backups had them, and a bank
        val card = UUID.randomUUID()
        val bank = UUID.randomUUID()
        val backup = """
            {
              "accounts": [
                {"name": "Visa", "color": 1, "type": "CREDIT_CARD", "creditLimit": 1500.0,
                 "chargeDay": 15, "statementClosingDay": 25, "id": "$card"},
                {"name": "Bank", "color": 2, "type": "BANK", "chargeDay": 3, "id": "$bank"}
              ],
              "transactions": [
                {"accountId": "$card", "type": "EXPENSE", "amount": 10.0},
                {"accountId": "$card", "type": "INCOME", "amount": 3.0},
                {"accountId": "$bank", "type": "EXPENSE", "amount": 7.0}
              ],
              "plannedPaymentRules": [
                {"startDate": 0, "intervalN": 1, "intervalType": "MONTH", "oneTime": false,
                 "type": "EXPENSE", "accountId": "$card", "amount": 12.0}
              ]
            }
        """.trimIndent()
        val transactionDao = FakeTransactionDao()
        val plannedPaymentDao = FakePlannedPaymentDao()
        val creditCardDao = FakeCreditCardDao()

        // when
        newBackupDataUseCase(
            transactionDao = transactionDao,
            plannedPaymentDao = plannedPaymentDao,
            creditCardDao = creditCardDao,
        ).importJson(backup)

        // then: the card account's card, under its id; its expenses are paid with it, nothing else
        creditCardDao.findAll() shouldBe listOf(
            CreditCardEntity(
                accountId = card,
                name = "Visa",
                chargeDay = 15,
                statementClosingDay = 25,
                creditLimit = 1500.0,
                id = card,
            )
        )
        transactionDao.findAll().associate { it.amount to it.creditCardId } shouldBe
            mapOf(10.0 to card, 3.0 to null, 7.0 to null)
        plannedPaymentDao.findAll().map { it.creditCardId } shouldBe listOf(card)
    }

    @Suppress("DEPRECATION") // the test is about the obsolete field
    @Test
    fun `rows a backup marks as deleted are not imported`() = runTest {
        // given
        val accountDao = FakeAccountDao()
        val transactionDao = FakeTransactionDao()
        val useCase = newBackupDataUseCase(accountDao = accountDao, transactionDao = transactionDao)
        val live = AccountEntity(name = "Live", color = 1)
        val deleted = AccountEntity(name = "Gone", color = 2, isDeleted = true)
        val backup = NivyCompleteData(
            accounts = listOf(live, deleted),
            transactions = listOf(
                TransactionEntity(accountId = live.id, type = TransactionType.INCOME, amount = 1.0),
                TransactionEntity(
                    accountId = deleted.id,
                    type = TransactionType.INCOME,
                    amount = 2.0,
                    isDeleted = true,
                ),
            ),
        )

        // when
        val result = useCase.importJson(json.encodeToString(backup))

        // then
        accountDao.findAll().map { it.id } shouldBe listOf(live.id)
        transactionDao.findAll().size shouldBe 1
        result.accountsImported shouldBe 1
        result.transactionsImported shouldBe 1
    }

    @Test
    fun `a backup's transfers to and from deleted accounts stay with the account still there`() = runTest {
        // given: a wallet with an account of its own, and a backup from before deleting an account
        // kept its transfers, two of which point to an account that's gone
        val accountDao = FakeAccountDao()
        val transactionDao = FakeTransactionDao(accounts = accountDao)
        val home = AccountEntity(name = "Home", color = 1)
        accountDao.save(home)
        val useCase = newBackupDataUseCase(accountDao = accountDao, transactionDao = transactionDao)
        val bank = AccountEntity(name = "Bank", color = 2)
        val deleted = UUID.randomUUID()
        val day = Instant.parse("2026-10-01T10:00:00Z")
        val toDeleted = TransactionEntity(
            accountId = bank.id,
            type = TransactionType.TRANSFER,
            amount = 60.0,
            toAccountId = deleted,
            toAmount = 50.0,
            dateTime = day,
        )
        val fromDeleted = TransactionEntity(
            accountId = deleted,
            type = TransactionType.TRANSFER,
            amount = 20.0,
            toAccountId = bank.id,
            toAmount = 24.0,
            dateTime = day,
        )
        val toHome = TransactionEntity(
            accountId = bank.id,
            type = TransactionType.TRANSFER,
            amount = 10.0,
            toAccountId = home.id,
            toAmount = 10.0,
            dateTime = day,
        )
        val backup = NivyCompleteData(accounts = listOf(bank), transactions = listOf(toDeleted, fromDeleted, toHome))

        // when
        useCase.importJson(json.encodeToString(backup))

        // then: the bank's expense and income, as the database keeps them; the transfer to the
        // wallet's own account stays a transfer
        transactionDao.findAll() shouldContainExactlyInAnyOrder listOf(
            toDeleted.copy(
                type = TransactionType.EXPENSE,
                toAccountId = null,
                toAmount = null,
                description = "Transfer to a deleted account",
            ),
            fromDeleted.copy(
                type = TransactionType.INCOME,
                accountId = bank.id,
                amount = 24.0,
                toAccountId = null,
                toAmount = null,
                description = "Transfer from a deleted account",
            ),
            toHome,
        )
        val balance = transactionDao.balance(bank.id, Instant.EPOCH, Instant.parse("3000-01-01T00:00:00Z"))
        balance shouldBe -46.0 // -60 + 24 - 10
    }

    private companion object {
        const val ZipSlip = "Zip entry outside the destination folder: ../databases/x.json"
        const val NoFile = "The provider gave no file to open"
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

/**
 * The balances of backups/nivy-2026-11.json's accounts, worked out by hand: the bank has the
 * salary, less the groceries, three transfers out and the paid rent (the rent still due counts
 * for nothing); the card has the card payment less the tickets.
 */
private val NivyBackupBalances = mapOf(
    "aaaaaaaa-0000-4000-8000-000000000001" to 340.0, // Bank: 2500 - 60 - 100 - 200 - 1000 - 800
    "aaaaaaaa-0000-4000-8000-000000000002" to -62.5, // Cash: -12.5 - 50
    "aaaaaaaa-0000-4000-8000-000000000003" to 80.0, // Amex: 200 - 120
    "aaaaaaaa-0000-4000-8000-000000000004" to 108.0, // Dollars: the 100 sent arrive as 108
    "aaaaaaaa-0000-4000-8000-000000000005" to 1000.0, // Brokerage
    "aaaaaaaa-0000-4000-8000-000000000006" to 500.0, // Old savings, archived
)
