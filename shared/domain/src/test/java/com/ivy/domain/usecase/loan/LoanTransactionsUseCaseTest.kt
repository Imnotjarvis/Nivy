package com.ivy.domain.usecase.loan

import arrow.core.Some
import com.ivy.base.model.LoanRecordType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.data.model.LoanRecord
import com.ivy.data.model.LoanRecordId
import com.ivy.data.model.LoanType
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.LoanRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class LoanTransactionsUseCaseTest {
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val loanRepository = mockk<LoanRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>()
    private val currencyRepository = mockk<CurrencyRepository>()
    private val exchangeUseCase = mockk<ExchangeUseCase>()
    private val timeProvider = mockk<TimeProvider>()

    private val useCase = LoanTransactionsUseCase(
        transactionRepository = transactionRepository,
        loanRepository = loanRepository,
        categoryRepository = categoryRepository,
        accountRepository = accountRepository,
        currencyRepository = currencyRepository,
        exchangeUseCase = exchangeUseCase,
        timeProvider = timeProvider,
    )

    init {
        coEvery { accountRepository.findById(BgnAccount.id) } returns BgnAccount
        coEvery { accountRepository.findById(EurAccount.id) } returns EurAccount
        coEvery { currencyRepository.getBaseCurrency() } returns Bgn
        coEvery { categoryRepository.findAll() } returns listOf(LoansCategory)
        coEvery { transactionRepository.findLoanTransaction(any()) } returns null
        coEvery { transactionRepository.findLoanRecordTransaction(any()) } returns null
        coEvery { timeProvider.utcNow() } returns Now
    }

    @Test
    fun `borrowing creates an income in the loan's account`() = runTest {
        // when
        useCase.syncLoanTransaction(BorrowedLoan, create = true)

        // then
        val saved = slot<Transaction>()
        coVerify { transactionRepository.save(capture(saved)) }
        val income = saved.captured.shouldBeInstanceOf<Income>()
        income.account shouldBe BgnAccount.id
        income.value shouldBe PositiveValue(PositiveDouble.unsafe(500.0), Bgn)
        income.title shouldBe BorrowedLoan.name
        income.category shouldBe LoansCategory.id
        income.time shouldBe Now
        income.metadata.loanId shouldBe BorrowedLoan.id.value
        income.metadata.loanRecordId shouldBe null
    }

    @Test
    fun `not creating deletes the existing loan transaction`() = runTest {
        // given
        val existing = mockk<Transaction>(relaxed = true)
        coEvery { transactionRepository.findLoanTransaction(BorrowedLoan.id.value) } returns existing

        // when
        useCase.syncLoanTransaction(BorrowedLoan, create = false)

        // then
        coVerify { transactionRepository.deleteById(existing.id) }
        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `paying back a borrowed loan is an expense`() = runTest {
        // given
        val record = record(type = LoanRecordType.DECREASE, account = BgnAccount.id)

        // when
        useCase.syncLoanRecordTransaction(BorrowedLoan, record, create = true)

        // then
        val saved = slot<Transaction>()
        coVerify { transactionRepository.save(capture(saved)) }
        val expense = saved.captured.shouldBeInstanceOf<Expense>()
        expense.time shouldBe record.dateTime
        expense.metadata.loanRecordId shouldBe record.id.value
    }

    @Test
    fun `record in the loan's currency has no converted amount`() = runTest {
        // when
        val converted = useCase.convertedAmount(
            loanAccount = BgnAccount.id,
            record = record(type = LoanRecordType.DECREASE, account = BgnAccount.id),
        )

        // then
        converted shouldBe null
    }

    @Test
    fun `new record in another currency is converted`() = runTest {
        // given
        coEvery {
            exchangeUseCase.convert(PositiveValue(PositiveDouble.unsafe(100.0), Eur), to = Bgn)
        } returns Some(PositiveValue(PositiveDouble.unsafe(195.0), Bgn))

        // when
        val converted = useCase.convertedAmount(
            loanAccount = BgnAccount.id,
            record = record(type = LoanRecordType.DECREASE, account = EurAccount.id),
        )

        // then
        converted shouldBe PositiveDouble.unsafe(195.0)
    }

    @Test
    fun `edited amount keeps the old rate`() = runTest {
        // given
        val old = record(type = LoanRecordType.DECREASE, account = EurAccount.id)
            .copy(convertedAmount = PositiveDouble.unsafe(200.0))
        val new = old.copy(amount = PositiveDouble.unsafe(50.0))

        // when
        val converted = useCase.convertedAmount(
            loanAccount = BgnAccount.id,
            record = new,
            oldRecord = old,
        )

        // then
        converted shouldBe PositiveDouble.unsafe(100.0)
    }

    private fun record(type: LoanRecordType, account: AccountId) = LoanRecord(
        id = LoanRecordId(UUID.randomUUID()),
        loan = BorrowedLoan.id,
        amount = PositiveDouble.unsafe(100.0),
        note = null,
        dateTime = Instant.parse("2024-02-01T10:00:00Z"),
        interest = false,
        account = account,
        convertedAmount = null,
        type = type,
    )

    companion object {
        val Now: Instant = Instant.parse("2024-03-01T10:00:00Z")
        val Bgn = AssetCode.unsafe("BGN")
        val Eur = AssetCode.unsafe("EUR")

        val BgnAccount = account("Bank", Bgn)
        val EurAccount = account("Revolut", Eur)

        val LoansCategory = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Loans"),
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
        )

        val BorrowedLoan = Loan(
            id = LoanId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Car"),
            amount = PositiveDouble.unsafe(500.0),
            type = LoanType.BORROW,
            color = ColorInt(0),
            icon = null,
            account = BgnAccount.id,
            note = null,
            dateTime = null,
            orderNum = 0.0,
        )

        private fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )
    }
}
