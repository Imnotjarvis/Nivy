package com.nivy.loans.loandetails

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.LoanType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.model.Transaction
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.loan.LoanTransactionsUseCase
import com.nivy.loans.LoanProgress
import com.nivy.loans.loandetails.events.DeleteLoanModalEvent
import com.nivy.loans.loandetails.events.LoanModalEvent
import com.nivy.loans.loandetails.events.LoanRecordModalEvent
import com.nivy.navigation.LoanDetailsScreen
import com.nivy.navigation.Navigation
import com.nivy.ui.testing.runTest
import com.nivy.wallet.domain.logic.model.CreateLoanRecordData
import com.nivy.wallet.domain.logic.model.EditLoanRecordData
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class LoanDetailsViewModelTest {

    private val bank = account("Bank", Eur)
    private val dollars = account("Dollars", Usd)
    private val loan = Loan(
        id = LoanId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Car"),
        amount = PositiveDouble.unsafe(1_000.0),
        type = LoanType.BORROW,
        color = ColorInt(0),
        icon = null,
        account = bank.id,
        note = null,
        dateTime = null,
        orderNum = 0.0,
    )
    private val payment = record(200.0, LoanRecordType.DECREASE, account = bank)
    private val interest = record(50.0, LoanRecordType.DECREASE, interest = true)
    private val moreLent = record(300.0, LoanRecordType.INCREASE)

    private var records = listOf(payment, interest, moreLent)
    private val loanRepository = mockk<LoanRepository>(relaxUnitFun = true) {
        coEvery { findById(loan.id) } answers { loan }
        coEvery { findRecordsByLoanId(loan.id) } answers { records }
        coEvery { saveRecord(any()) } answers {
            val saved = firstArg<LoanRecord>()
            records = records.filterNot { it.id == saved.id } + saved
        }
    }
    private val paymentTransaction = mockk<Transaction>()
    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findLoanRecordTransaction(any()) } answers {
            paymentTransaction.takeIf { firstArg<UUID>() == payment.id.value }
        }
        coEvery { findLoanTransaction(any()) } returns null
    }

    // A record in dollars counts at 1 USD = 0.5 EUR toward the loan, kept in euros.
    private val loanTransactionsUseCase = mockk<LoanTransactionsUseCase>(relaxUnitFun = true) {
        coEvery { convertedAmount(any(), any(), any(), any()) } answers {
            val record = secondArg<LoanRecord>()
            if (record.account == dollars.id) PositiveDouble.unsafe(record.amount.value / 2) else null
        }
    }
    private val nav = mockk<Navigation>(relaxed = true)

    private val viewModel = LoanDetailsViewModel(
        loanRepository = loanRepository,
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        transactionRepository = transactionRepository,
        accountCreator = mockk(relaxed = true),
        loanTransactionsUseCase = loanTransactionsUseCase,
        nav = nav,
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns listOf(bank, dollars) },
        timeConverter = TestTimeConverter(),
        timeProvider = FixedTime,
        dateTimePicker = mockk(relaxed = true),
        dispatchers = TestDispatchersProvider,
    ).also { it.screen = LoanDetailsScreen(loanId = loan.id.value) }

    @Test
    fun `a loan's page shows its records with their accounts, and how much is paid back`() {
        // when
        viewModel.runTest {
            // then
            baseCurrency shouldBe "EUR"
            displayLoanRecords.map { it.loanRecord } shouldBe listOf(payment, interest, moreLent)
            displayLoanRecords.first().account shouldBe bank
            displayLoanRecords.map { it.loanRecordTransaction } shouldBe listOf(true, false, false)
            progress shouldBe LoanProgress(total = 1_300.0, paid = 200.0, interestPaid = 50.0)
        }
    }

    @Test
    fun `a new record is saved converted to the loan's currency, with its transaction, and counted`() {
        // given
        val newPayment = CreateLoanRecordData(
            note = "Second",
            amount = 100.0,
            dateTime = Instant.parse("2026-11-10T10:00:00Z"),
            account = dollars,
            createLoanRecordTransaction = true,
            loanRecordType = LoanRecordType.DECREASE,
        )

        // when
        viewModel.runTest(events = listOf(LoanRecordModalEvent.OnCreateLoanRecord(newPayment))) {
            // then
            progress.paid shouldBe 250.0
        }
        val saved = records.last()
        saved.amount shouldBe PositiveDouble.unsafe(100.0)
        saved.convertedAmount shouldBe PositiveDouble.unsafe(50.0)
        saved.account shouldBe dollars.id
        coVerify(exactly = 1) { loanTransactionsUseCase.syncLoanRecordTransaction(loan, saved, create = true) }
    }

    @Test
    fun `a record without a positive amount isn't saved`() {
        // given
        val nothing = CreateLoanRecordData(
            note = null,
            amount = 0.0,
            dateTime = Instant.parse("2026-11-10T10:00:00Z"),
            loanRecordType = LoanRecordType.DECREASE,
        )

        // when
        viewModel.runTest(events = listOf(LoanRecordModalEvent.OnCreateLoanRecord(nothing))) {}

        // then
        coVerify(exactly = 0) { loanRepository.saveRecord(any()) }
        coVerify(exactly = 0) { loanTransactionsUseCase.syncLoanRecordTransaction(any(), any(), any()) }
    }

    @Test
    fun `an edited record is converted from the original, recalculated only when asked`() {
        // given
        val edited = payment.copy(amount = PositiveDouble.unsafe(250.0))
        val oldRecord = slot<LoanRecord>()
        val recalculate = slot<Boolean>()
        coEvery {
            loanTransactionsUseCase.convertedAmount(any(), edited, capture(oldRecord), capture(recalculate))
        } returns null

        // when
        viewModel.runTest(
            events = listOf(
                LoanRecordModalEvent.OnEditLoanRecord(
                    EditLoanRecordData(
                        newLoanRecord = edited,
                        originalLoanRecord = payment,
                        reCalculateLoanAmount = true,
                    )
                )
            )
        ) {
            // then
            progress.paid shouldBe 250.0
        }
        oldRecord.captured shouldBe payment
        recalculate.captured shouldBe true
        coVerify(exactly = 1) { loanTransactionsUseCase.syncLoanRecordTransaction(loan, edited, create = false) }
    }

    @Test
    fun `deleting a record deletes its transaction too, and reloads the page`() {
        // when
        viewModel.runTest(events = listOf(LoanRecordModalEvent.OnDeleteLoanRecord(payment))) {}

        // then
        coVerify(exactly = 1) { loanRepository.deleteRecordById(payment.id) }
        coVerify(exactly = 1) { loanTransactionsUseCase.deleteLoanRecordTransaction(payment.id) }
        coVerify(exactly = 2) { loanRepository.findRecordsByLoanId(loan.id) }
    }

    @Test
    fun `editing the loan recalculates records from the old account, syncs transactions and closes the wait`() {
        // given
        val moved = loan.copy(account = dollars.id)

        // when
        viewModel.runTest(
            events = listOf(LoanModalEvent.PerformCalculation, LoanModalEvent.OnEditLoanModal(moved, true))
        ) {
            // then
            waitModalVisible shouldBe false
        }
        coVerifyOrder {
            loanRepository.save(moved)
            loanTransactionsUseCase.recalculateRecords(moved, oldAccount = bank.id)
            loanTransactionsUseCase.syncLoanTransaction(moved, create = true)
            loanTransactionsUseCase.syncRecordTransactions(moved)
        }
    }

    @Test
    fun `deleting the loan deletes its transactions, then the loan, and leaves its page`() {
        // when
        viewModel.runTest(events = listOf(DeleteLoanModalEvent.OnDeleteLoan)) {
            // then
            isDeleteModalVisible shouldBe false
        }
        coVerifyOrder {
            loanTransactionsUseCase.deleteLoanTransactions(loan.id)
            loanRepository.deleteById(loan.id)
            nav.back()
        }
    }

    private fun record(
        amount: Double,
        type: LoanRecordType,
        interest: Boolean = false,
        account: Account? = null,
    ) = LoanRecord(
        id = LoanRecordId(UUID.randomUUID()),
        loan = loan.id,
        amount = PositiveDouble.unsafe(amount),
        note = null,
        dateTime = Instant.parse("2026-11-01T10:00:00Z"),
        interest = interest,
        account = account?.id,
        convertedAmount = null,
        type = type,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

        /** November 15, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
