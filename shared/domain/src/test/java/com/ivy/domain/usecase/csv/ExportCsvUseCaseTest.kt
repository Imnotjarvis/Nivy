package com.ivy.domain.usecase.csv

import arrow.core.Some
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.file.FileSystem
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.testing.account
import com.ivy.data.model.testing.income
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.TransactionRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import com.opencsv.CSVReaderBuilder
import com.opencsv.RFC4180ParserBuilder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.StringReader

class ExportCsvUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val transactionRepository = mockk<TransactionRepository>()

    private val useCase = ExportCsvUseCase(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        transactionRepository = transactionRepository,
        dispatchers = TestDispatchersProvider,
        fileSystem = mockk<FileSystem>(),
        timeConverter = TestTimeConverter()
    )

    @Test
    fun `text that a spreadsheet would run as a formula is exported as text`() = runTest {
        // given a transaction whose title looks like a formula
        val income = Arb.income().next().copy(
            title = NotBlankTrimmedString.unsafe("=HYPERLINK(\"http://evil\",\"click\")"),
            description = NotBlankTrimmedString.unsafe("-5 \\ back\\slashes"),
        )
        coEvery { accountRepository.findAll() } returns listOf(
            Arb.account(accountId = Some(income.account)).next()
        )

        // when
        val csv = useCase.exportCsv { listOf(income) }

        // then the raw cells can't start a formula
        val rawRow = csv.lines()[1]
        val titleColumn = IvyCsvRow.Columns.indexOf("Title")
        val descriptionColumn = IvyCsvRow.Columns.indexOf("Description")
        val rawCells = rawCells(rawRow)
        rawCells[titleColumn] shouldStartWith "'="
        rawCells[descriptionColumn] shouldStartWith "'-"

        // and the app's own reader gives the original values back, backslashes included
        val row = ReadCsvUseCase().readCsv(csv, skipLines = 1).single()
        row[titleColumn] shouldBe income.title?.value
        row[descriptionColumn] shouldBe income.description?.value
    }

    @Test
    fun `crypto amounts keep their decimals and fiat amounts keep two`() = runTest {
        // given
        val btc = Arb.income().next().copy(
            value = PositiveValue(PositiveDouble.unsafe(0.00042), AssetCode.unsafe("BTC"))
        )
        val eur = Arb.income().next().copy(
            value = PositiveValue(PositiveDouble.unsafe(1234.5), AssetCode.unsafe("EUR"))
        )
        coEvery { accountRepository.findAll() } returns listOf(
            Arb.account(accountId = Some(btc.account)).next(),
            Arb.account(accountId = Some(eur.account)).next(),
        )

        // when
        val csv = useCase.exportCsv { listOf(btc, eur) }

        // then
        val amountColumn = IvyCsvRow.Columns.indexOf("Amount")
        ReadCsvUseCase().readCsv(csv, skipLines = 1).map { it[amountColumn] } shouldBe
            listOf("0.00042", "1,234.50")
    }

    /** The cells as written to the file, without the guard's apostrophe being taken off. */
    private fun rawCells(line: String): List<String> = CSVReaderBuilder(StringReader(line))
        .withCSVParser(RFC4180ParserBuilder().build())
        .build()
        .readAll()
        .single()
        .toList()
}
