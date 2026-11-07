package com.nivy.importdata.csv.domain

import com.nivy.base.model.AccountType
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.mapper.TransactionMapper
import com.nivy.importdata.csv.CSVRow
import com.nivy.importdata.csv.ColumnMapping
import com.nivy.importdata.csv.DateMetadata
import com.nivy.importdata.csv.ImportantFields
import com.nivy.importdata.csv.MappingStatus
import com.nivy.importdata.csv.OptionalFields
import com.nivy.importdata.csv.TransferFields
import com.nivy.importdata.csv.TrnTypeMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class CSVImporterV2Test {

    private val bank = account("Bank", "EUR")
    private val savings = account("Savings", "EUR")
    private val dollars = account("Dollars", "USD")

    private val accountRepository = mockk<AccountRepository> {
        coEvery { findAll() } returns listOf(bank, savings, dollars)
        listOf(bank, savings, dollars).forEach { coEvery { findById(it.id) } returns it }
    }
    private val saved = mutableListOf<Transaction>()

    // Notes whether each transaction is saved inside the database transaction.
    private var inTransaction = false
    private val savedInTransaction = mutableListOf<Boolean>()
    private val transaction = object : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R {
            inTransaction = true
            return block().also { inTransaction = false }
        }
    }

    private val importer = CSVImporterV2(
        transactionRepository = mockk<TransactionRepository> {
            coEvery { save(capture(saved)) } answers { savedInTransaction += inTransaction }
        },
        transactionMapper = TransactionMapper(accountRepository),
        accountDao = mockk(),
        categoryRepository = mockk { coEvery { findAll() } returns emptyList() },
        currencyRepository = mockk { coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR") },
        accountRepository = accountRepository,
        timeConverter = TestTimeConverter(),
        transaction = transaction,
    )

    @Test
    fun `a transfer between two currencies with no To Amount is skipped and listed, not taken one for one`() =
        runTest {
            val result = import(transfer(from = "Bank", to = "Dollars", amount = "100", toAmount = ""))

            result.transactionsImported shouldBe 0
            result.failedRows.single().index shouldBe 2
            saved shouldBe emptyList()
        }

    @Test
    fun `an import is all or nothing, in one database transaction`() = runTest {
        import(
            transfer(from = "Bank", to = "Savings", amount = "100", toAmount = ""),
            transfer(from = "Bank", to = "Dollars", amount = "100", toAmount = "108"),
        )

        savedInTransaction shouldBe listOf(true, true)
    }

    @Test
    fun `a transfer between two currencies arrives as its To Amount`() = runTest {
        import(transfer(from = "Bank", to = "Dollars", amount = "100", toAmount = "108"))

        (saved.single() as Transfer).toValue.amount.value shouldBe 108.0
    }

    @Test
    fun `a transfer in one currency with no To Amount arrives as it left`() = runTest {
        import(transfer(from = "Bank", to = "Savings", amount = "100", toAmount = ""))

        (saved.single() as Transfer).toValue.amount.value shouldBe 100.0
    }

    private suspend fun import(vararg rows: List<String>) = importer.import(
        csv = (listOf(Header) + rows).map(::CSVRow),
        importantFields = ImportantFields(
            amount = column(index = 2, metadata = 1),
            amountStatus = Ok,
            type = column(index = 1, metadata = TrnTypeMetadata("Income", "Expense", "Transfer")),
            typeStatus = Ok,
            date = column(index = 0, metadata = DateMetadata.DateFirst),
            dateStatus = Ok,
            account = column(index = 3, metadata = Unit),
            accountStatus = Ok,
            accountCurrency = column(index = NoColumn, metadata = Unit),
            accountCurrencyStatus = Ok,
        ),
        transferFields = TransferFields(
            toAccount = column(index = 4, metadata = Unit),
            toAccountStatus = Ok,
            toAccountCurrency = column(index = NoColumn, metadata = Unit),
            toAccountCurrencyStatus = Ok,
            toAmount = column(index = 5, metadata = 1),
            toAmountStatus = Ok,
        ),
        optionalFields = OptionalFields(
            category = column(index = NoColumn, metadata = Unit),
            categoryStatus = Ok,
            title = column(index = NoColumn, metadata = Unit),
            titleStatus = Ok,
            description = column(index = NoColumn, metadata = Unit),
            descriptionStatus = Ok,
        ),
        onProgress = {},
    )

    private fun transfer(from: String, to: String, amount: String, toAmount: String) =
        listOf("2026-10-05 10:00", "Transfer", amount, from, to, toAmount)

    private fun <M> column(index: Int, metadata: M) = ColumnMapping(
        nivyColumn = 0,
        helpInfo = 0,
        name = "",
        index = index,
        metadata = metadata,
        required = false,
    )

    private fun account(name: String, currency: String) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = AssetCode.unsafe(currency),
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )

    private companion object {
        val Header = listOf("Date", "Type", "Amount", "Account", "To Account", "To Amount")
        val Ok = MappingStatus(sampleValues = emptyList(), success = true)

        /** A field the file has no column for. */
        const val NoColumn = -1
    }
}
