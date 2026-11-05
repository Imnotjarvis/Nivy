package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.DataObserver
import com.ivy.base.model.TransactionType
import com.ivy.data.db.DatabaseTransaction
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeBudgetDao
import com.ivy.data.db.dao.fake.FakeCategoryDao
import com.ivy.data.db.dao.fake.FakeCreditCardDao
import com.ivy.data.db.dao.fake.FakeLoanDao
import com.ivy.data.db.dao.fake.FakeLoanRecordDao
import com.ivy.data.db.dao.fake.FakePlannedPaymentDao
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.preferences.InMemoryPreferencesDataStore
import com.ivy.data.db.dao.fake.FakeTagAssociationDao
import com.ivy.data.db.dao.fake.FakeTagDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.testResource
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Test
import java.util.UUID

class BackupDataUseCaseTest {
    private val json = KotlinxSerializationModule.provideJson()

    /** The fakes have no transactions; the block simply runs. */
    private class FakeDatabaseTransaction : DatabaseTransaction(mockk()) {
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

            context = mockk(relaxed = true),
            appPreferences = appPreferences,
            json = json,
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = mockk(relaxed = true),
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            creditCardDao = creditCardDao,
            creditCardWriter = creditCardDao,
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
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
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
        val backup = IvyWalletCompleteData(accounts = listOf(cashEur, cashUsd))

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
        val backup = IvyWalletCompleteData(
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
            val backup = IvyWalletCompleteData(
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
        useCase.importJson(json.encodeToString(IvyWalletCompleteData(accounts = listOf(backupCash))))

        // then: the existing account keeps its currency, and the backup's arrives beside it
        accountDao.findAll().associate { it.id to it.currency } shouldBe
            mapOf(existingCash.id to "EUR", backupCash.id to "USD")
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
        creditCardDao.save(visa)
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
        val backup = IvyWalletCompleteData(
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
}
