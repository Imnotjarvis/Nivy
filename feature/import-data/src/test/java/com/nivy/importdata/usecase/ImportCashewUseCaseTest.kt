package com.nivy.importdata.usecase

import android.content.Context
import android.net.Uri
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.file.FileSystem
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.planned.PlannedPaymentsGenerator
import com.nivy.importdata.cashew.CashewFile
import com.nivy.importdata.cashew.CashewFileReader
import com.nivy.importdata.cashew.CashewRepeat
import com.nivy.importdata.cashew.CashewType
import com.nivy.importdata.cashew.Now
import com.nivy.importdata.cashew.readCashew
import com.nivy.ui.R
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.ZoneOffset
import java.util.UUID

class ImportCashewUseCaseTest {
    private val cache: File = Files.createTempDirectory("cache").toFile()
    private val cashew = CashewFile()
    private val uri = mockk<Uri>()

    // Every write, and whether it was made inside the import's database transaction.
    private val writes = mutableListOf<String>()
    private var inTransaction = false
    private fun write(name: String) {
        writes += if (inTransaction) name else "$name, outside the transaction"
    }

    private var existingAccounts = emptyList<Account>()
    private var databaseFull = false
    private val savedTransactions = mutableListOf<List<Transaction>>()
    private val appPreferences = mockk<AppPreferences>(relaxUnitFun = true)

    private val useCase = ImportCashewUseCase(
        context = mockk<Context> {
            every { cacheDir } returns cache
            every { getString(R.string.cashew_not_data_file) } returns "Not a Cashew data file"
        },
        fileSystem = mockk<FileSystem> {
            every { copy(uri, any()) } answers { secondArg<File>().writeText("Cashew's database") }
        },
        cashewFile = mockk<CashewFileReader> { every { read(any()) } answers { readCashew(cashew.tables) } },
        accountRepository = mockk {
            coEvery { findAll() } answers { existingAccounts }
            coEvery { saveMany(any()) } answers { write("accounts") }
        },
        categoryRepository = mockk {
            coEvery { findAll() } returns emptyList()
            coEvery { save(any()) } answers { write("category") }
        },
        tagRepository = mockk<TagRepository> {
            coEvery { findAll() } returns emptyList()
            coEvery { save(any()) } answers { write("tag") }
            coEvery { associateMany(any()) } answers { write("tag links") }
        },
        transactionRepository = mockk<TransactionRepository> {
            coEvery { saveMany(any()) } answers {
                check(!databaseFull) { "database or disk is full" }
                savedTransactions += firstArg<List<Transaction>>()
                write("transactions")
            }
        },
        plannedPaymentRuleRepository = mockk<PlannedPaymentRuleRepository> {
            coEvery { save(any()) } answers { write("planned payment") }
        },
        plannedPaymentsGenerator = mockk<PlannedPaymentsGenerator> {
            coEvery { generate(any(), previous = null) } answers { write("its occurrences") }
        },
        loanRepository = mockk<LoanRepository> {
            coEvery { findMaxOrderNum() } returns 0.0
            coEvery { saveMany(any()) } answers { write("loans") }
            coEvery { saveRecord(any()) } answers { write("loan record") }
        },
        budgetRepository = mockk<BudgetRepository> {
            coEvery { findMaxOrderNum() } returns 0.0
            coEvery { saveMany(any()) } answers { write("budgets") }
        },
        exchangeRatesRepository = mockk<ExchangeRatesRepository> {
            coEvery { findAllManuallyOverridden() } returns emptyList()
            coEvery { save(any()) } answers { write("rate") }
        },
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns AssetCode.EUR },
        appPreferences = appPreferences,
        timeProvider = mockk<TimeProvider> {
            every { getZoneId() } returns ZoneOffset.UTC
            every { utcNow() } returns Now
        },
        dispatchers = TestDispatchersProvider,
        databaseTransaction = object : DatabaseTransaction(mockk(), mockk()) {
            override suspend fun <R> run(block: suspend () -> R): R {
                inTransaction = true
                return try {
                    block()
                } finally {
                    inTransaction = false
                }
            }
        },
        dataObserver = mockk<DataObserver>(relaxUnitFun = true),
    )

    @After
    fun tearDown() {
        cashew.close()
        cache.deleteRecursively()
    }

    @Test
    fun `a Cashew file comes over in one database transaction, and the result counts it`() = runTest {
        // given
        cashew.apply {
            wallet("w1", "Bank", currency = "eur")
            wallet("w2", "Dollars", currency = "usd")
            category("c1", "Dining")
            category("c2", "Coffee", main = "c1")
            transaction("t1", -3.5, subCategory = "c2", date = "2026-10-01T08:00:00Z")
            transaction("t2", 2000.0, wallet = "w2", date = "2026-10-02T08:00:00Z")
            transaction(
                "s1", -9.99, type = CashewType.Subscription, repeat = CashewRepeat.Monthly, repeatEvery = 1,
                paid = false, date = "2026-11-05T08:00:00Z",
            )
            transaction("l1", -50.0, type = CashewType.Credit, paid = false)
            budget("b1", "Food", 300.0)
            settings("""{"selectedWalletPk":"w1","customCurrencyAmounts":{"usd":1.1},"cachedCurrencyExchange":{"eur":0.9}}""")
        }

        // when
        val imported = useCase.import(uri) {}

        // then
        writes shouldBe listOf(
            "accounts", "category", "tag", "transactions", "tag links", "loans", "loan record",
            "planned payment", "its occurrences", "budgets", "rate",
        )
        with(imported.result) {
            rowsFound shouldBe 2
            transactionsImported shouldBe 2
            accountsImported shouldBe 2
            categoriesImported shouldBe 1
            error shouldBe null
            succeeded shouldBe true
        }
        imported.plannedPayments shouldBe 1
        imported.loans shouldBe 1
        imported.budgets shouldBe 1
        imported.tags shouldBe 1
        imported.leftOut.shouldBeEmpty()
        savedTransactions.single().size shouldBe 2
        // The copy of the file is gone.
        cache.listFiles().orEmpty().toList().shouldBeEmpty()
    }

    @Test
    fun `a file that isn't Cashew's fails, says so, and writes nothing`() = runTest {
        // given a database without Cashew's wallets

        // when
        val imported = useCase.import(uri) {}

        // then
        imported.result.error shouldBe "Not a Cashew data file"
        imported.result.succeeded shouldBe false
        writes.shouldBeEmpty()
    }

    @Test
    fun `a write the database refuses fails the import, with the reason`() = runTest {
        // given
        cashew.apply {
            wallet("w1", "Bank", currency = "usd")
            category("c1", "Dining")
            transaction("t1", -3.5)
            settings("""{"selectedWalletPk":"w1"}""")
        }
        databaseFull = true

        // when
        val imported = useCase.import(uri) {}

        // then
        imported.result.error shouldBe "database or disk is full"
        // The wallet keeps its currency: nothing came in.
        coVerify(exactly = 0) { appPreferences.setBaseCurrency(any()) }
    }

    @Test
    fun `a wallet without accounts takes Cashew's main currency, one with accounts keeps its own`() = runTest {
        // given
        cashew.apply {
            wallet("w1", "Dollars", currency = "usd")
            category("c1", "Dining")
            transaction("t1", -3.5)
            settings("""{"selectedWalletPk":"w1"}""")
        }

        // when
        useCase.import(uri) {}

        // then
        coVerify(exactly = 1) { appPreferences.setBaseCurrency("USD") }

        // when the wallet has an account already
        existingAccounts = listOf(account("Cash"))
        useCase.import(uri) {}

        // then
        coVerify(exactly = 1) { appPreferences.setBaseCurrency(any()) }
    }

    private fun account(name: String) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = AssetCode.EUR,
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.CASH,
        archived = false,
        orderNum = 0.0,
    )
}
