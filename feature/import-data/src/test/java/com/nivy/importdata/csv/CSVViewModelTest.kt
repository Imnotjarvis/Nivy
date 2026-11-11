package com.nivy.importdata.csv

import android.database.sqlite.SQLiteException
import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.Toaster
import com.nivy.data.backup.ImportResult
import com.nivy.data.file.FileSystem
import com.nivy.domain.usecase.csv.ReadCsvUseCase
import com.nivy.importdata.csv.domain.CSVImporterV2
import com.nivy.navigation.Navigation
import com.nivy.onboarding.viewmodel.OnboardingViewModel
import com.nivy.ui.R
import com.nivy.ui.testing.runComposeTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CSVViewModelTest {

    private val uri = mockk<Uri>()
    private var file = BankExport
    private var unreadable = false
    private val fileSystem = mockk<FileSystem> {
        every { read(uri, any()) } answers {
            if (unreadable) FileSystem.Failure.IO(IOException()).left() else file.right()
        }
    }
    private val imported = ImportResult(
        rowsFound = 2,
        transactionsImported = 2,
        accountsImported = 1,
        categoriesImported = 0,
        failedRows = persistentListOf(),
    )
    private val importedFields = slot<ImportantFields>()
    private val csvImporter = mockk<CSVImporterV2> {
        coEvery { import(any(), capture(importedFields), any(), any(), any()) } returns imported
    }
    private val nav = mockk<Navigation>(relaxed = true)
    private val toaster = mockk<Toaster>(relaxed = true)
    private lateinit var viewModel: CSVViewModel

    @Before
    fun setUp() {
        // The view model starts collecting its events as it's created.
        Dispatchers.setMain(Dispatchers.Unconfined)
        viewModel = CSVViewModel(
            fileReader = fileSystem,
            readCsv = ReadCsvUseCase(),
            csvImporter = csvImporter,
            nav = nav,
            dispatchers = TestDispatchersProvider,
            toaster = toaster,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun csv(vararg events: CSVEvent, verify: CSVState.() -> Unit = {}) = csv(events.toList(), verify)

    private fun csv(events: List<CSVEvent>, verify: CSVState.() -> Unit = {}) = runComposeTest(
        present = { viewModel.uiState() },
        actions = { events.forEach(viewModel::onEvent) },
        verify = verify,
    )

    /** The file, with the four columns an import needs mapped. */
    private val pickedAndMapped
        get() = listOf(
            CSVEvent.FilePicked(uri),
            CSVEvent.MapDate(0, "Date"),
            CSVEvent.MapAmount(1, "Amount"),
            CSVEvent.MapType(2, "Type"),
            CSVEvent.MapAccount(3, "Account"),
        )

    @Test
    fun `a picked file shows its columns, and its values without spaces around them`() {
        // when
        csv(CSVEvent.FilePicked(uri)) {
            // then
            columns shouldBe CSVRow(listOf("Date", "Amount", "Type", "Account", "Title"))
            csv?.get(1) shouldBe CSVRow(listOf("01/11/2026 10:00", "12.50", "Expense", "Bank", "Coffee, croissant"))
        }
    }

    @Test
    fun `a file split by semicolons is read with them, decimal commas and all`() {
        // given
        file = "Date;Amount;Type;Account\n01/11/2026 10:00;12,50;Expense;Bank\n"

        // when
        csv(CSVEvent.FilePicked(uri), CSVEvent.MapAmount(1, "Amount")) {
            // then
            columns shouldBe CSVRow(listOf("Date", "Amount", "Type", "Account"))
            important?.amountStatus shouldBe MappingStatus(listOf("12.5"), success = true)
        }
    }

    @Test
    fun `a file that can't be read shows nothing, and says so`() {
        // given
        unreadable = true

        // when
        csv(CSVEvent.FilePicked(uri)) {
            // then
            csv shouldBe null
            columns shouldBe null
        }
        coVerify { toaster.show(R.string.csv_file_not_read, any()) }
    }

    @Test
    fun `another file picked starts with no column mapped`() {
        // given a file with its columns mapped, and one with other columns picked after it
        csv(pickedAndMapped + CSVEvent.MapTitle(4, "Title"))
        file = "Account,Title,Date,Type,Amount\nBank,Coffee,01/11/2026 10:00,Expense,12.50\n"

        // when
        csv(CSVEvent.FilePicked(uri)) {
            // then
            important?.amount?.index shouldBe NoColumn
            important?.account?.index shouldBe NoColumn
            optional?.title?.index shouldBe NoColumn
            continueEnabled shouldBe false
        }
    }

    @Test
    fun `each mapped column shows what it reads from the sample`() {
        // when
        csv(pickedAndMapped + CSVEvent.MapTitle(4, "Title")) {
            // then
            important?.amountStatus shouldBe MappingStatus(listOf("12.5", "1000.0"), success = true)
            important?.typeStatus shouldBe MappingStatus(listOf("EXPENSE", "INCOME"), success = true)
            optional?.titleStatus shouldBe MappingStatus(listOf("Coffee, croissant", "Salary"), success = true)
        }
    }

    @Test
    fun `the sample reads a file written as Cashew writes it as the import will`() {
        // given dates to the millisecond, and an amount with three decimals among amounts with one
        file = """
            account,amount,currency,title,date
            Bank,-12.0,EUR,Lunch,2026-10-05 13:00:00.000
            Bank,-4.125,EUR,Coffee,2026-10-06 09:30:00.000
        """.trimIndent()

        // when
        csv(CSVEvent.FilePicked(uri), CSVEvent.MapAmount(1, "amount"), CSVEvent.MapDate(4, "date")) {
            // then
            important?.amountStatus shouldBe MappingStatus(listOf("12.0", "4.125"), success = true)
            important?.dateStatus shouldBe MappingStatus(listOf("2026-10-05T13:00", "2026-10-06T09:30"), success = true)
        }
    }

    @Test
    fun `a column that reads nothing shows it, and continuing waits for the four it needs`() {
        // when
        csv(pickedAndMapped + CSVEvent.MapAmount(3, "Account")) {
            // then
            important?.amountStatus?.success shouldBe false
            continueEnabled shouldBe false
        }
        csv(CSVEvent.MapAmount(1, "Amount")) {
            continueEnabled shouldBe true
        }
    }

    @Test
    fun `continuing imports the rows with the mapped columns, and shows the result`() {
        // when
        csv(pickedAndMapped + CSVEvent.AmountMultiplier(100) + CSVEvent.Continue) {
            // then
            uiState shouldBe UIState.Result(imported)
        }
        importedFields.captured.amount.index shouldBe 1
        importedFields.captured.amount.metadata shouldBe 100
        importedFields.captured.account.index shouldBe 3
        coVerify(exactly = 1) { csvImporter.import(match { it.size == 3 }, any(), any(), any(), any()) }
    }

    @Test
    fun `an import that fails says so, and shows the columns again to try once more`() {
        coEvery { csvImporter.import(any(), any(), any(), any(), any()) } throws SQLiteException("disk I/O error")

        csv(pickedAndMapped + CSVEvent.Continue) {
            uiState shouldBe UIState.Idle
            columns shouldNotBe null
        }
        coVerify { toaster.show(R.string.msg_import_failed, any()) }
    }

    @Test
    fun `finishing an import started from onboarding tells it whether anything came in, and leaves both screens`() {
        // given
        val onboarding = mockk<OnboardingViewModel>(relaxed = true)
        csv(pickedAndMapped + CSVEvent.Continue)

        // when
        csv(CSVEvent.FinishImport(launchedFromOnboarding = true, onboardingViewModel = onboarding)) {
            // then
            uiState shouldBe UIState.Idle
            csv shouldBe null
        }
        verify(exactly = 1) { onboarding.importFinished(success = true) }
        verify(exactly = 2) { nav.back() }
    }

    @Test
    fun `an import that brought in less than half the rows isn't a success for onboarding either`() {
        // given 1 of 10 rows imported: the result page says it failed
        coEvery { csvImporter.import(any(), any(), any(), any(), any()) } returns
            imported.copy(rowsFound = 10, transactionsImported = 1)
        val onboarding = mockk<OnboardingViewModel>(relaxed = true)
        csv(pickedAndMapped + CSVEvent.Continue)

        // when
        csv(CSVEvent.FinishImport(launchedFromOnboarding = true, onboardingViewModel = onboarding))

        // then
        verify(exactly = 1) { onboarding.importFinished(success = false) }
    }

    @Test
    fun `finishing an import started from the settings leaves the import screen only`() {
        // given
        val onboarding = mockk<OnboardingViewModel>(relaxed = true)
        csv(pickedAndMapped + CSVEvent.Continue)

        // when
        csv(CSVEvent.FinishImport(launchedFromOnboarding = false, onboardingViewModel = onboarding))

        // then
        verify(exactly = 0) { onboarding.importFinished(any()) }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        /** A field no column is mapped to. */
        const val NoColumn = -1

        val BankExport = """
            Date,Amount,Type,Account,Title
            01/11/2026 10:00, 12.50 ,Expense,Bank,"Coffee, croissant"
            02/11/2026 09:30,1000,Income, Bank ,Salary
        """.trimIndent()
    }
}
