package com.nivy.domain.usecase.loan

import com.nivy.base.model.AccountType
import arrow.core.None
import arrow.core.Some
import com.nivy.base.model.LoanRecordType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.base.model.LoanType
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.assertions.throwables.shouldThrow
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
    private val creditCardRepository = mockk<CreditCardRepository>()

    private val useCase = LoanTransactionsUseCase(
        transactionRepository = transactionRepository,
        loanRepository = loanRepository,
        categoryRepository = categoryRepository,
        accountRepository = accountRepository,
        currencyRepository = currencyRepository,
        exchangeUseCase = exchangeUseCase,
        timeProvider = timeProvider,
        creditCardChargeUseCase = CreditCardChargeUseCase(creditCardRepository, TestTimeConverter()),
    )

    init {
        coEvery { accountRepository.findById(BgnAccount.id) } returns BgnAccount
        coEvery { accountRepository.findById(EurAccount.id) } returns EurAccount
        coEvery { accountRepository.findById(UsdAccount.id) } returns UsdAccount
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
    fun `a loan with a date dates its transaction by it`() = runTest {
        // given
        coEvery { timeProvider.getZoneId() } returns java.time.ZoneOffset.UTC
        val loan = BorrowedLoan.copy(dateTime = java.time.LocalDateTime.of(2024, 1, 15, 10, 0))

        // when
        useCase.syncLoanTransaction(loan, create = true)

        // then
        val saved = slot<Transaction>()
        coVerify { transactionRepository.save(capture(saved)) }
        saved.captured.time shouldBe Instant.parse("2024-01-15T10:00:00Z")
    }

    @Test
    fun `editing a loan keeps its transaction's card, charged on the card's day`() = runTest {
        // given: lent on 3 February, paid with a card that charges on the 10th, so on 10 March
        coEvery { timeProvider.getZoneId() } returns java.time.ZoneOffset.UTC
        val card = CreditCard(
            id = CreditCardId(UUID.randomUUID()),
            account = BgnAccount.id,
            name = NotBlankTrimmedString.unsafe("Visa"),
            chargeDay = 10,
            statementClosingDay = null,
            creditLimit = null,
        )
        coEvery { creditCardRepository.findById(card.id) } returns card
        val purchase = Instant.parse("2024-02-03T10:00:00Z")
        val loan = BorrowedLoan.copy(type = LoanType.LEND, dateTime = java.time.LocalDateTime.of(2024, 2, 3, 10, 0))
        coEvery { transactionRepository.findLoanTransaction(loan.id.value) } returns Expense(
            id = TransactionId(UUID.randomUUID()),
            title = loan.name,
            description = null,
            category = LoansCategory.id,
            time = Instant.parse("2024-03-10T10:00:00Z"),
            settled = true,
            metadata = TransactionMetadata(
                recurringRuleId = null,
                paidForDateTime = null,
                loanId = loan.id.value,
                loanRecordId = null,
                purchaseDateTime = purchase,
                creditCard = card.id,
            ),
            tags = emptyList(),
            value = PositiveValue(loan.amount, Bgn),
            account = BgnAccount.id,
        )

        // when: it's renamed
        useCase.syncLoanTransaction(loan.copy(name = NotBlankTrimmedString.unsafe("Car, half")), create = true)

        // then
        val saved = slot<Transaction>()
        coVerify { transactionRepository.save(capture(saved)) }
        saved.captured.metadata.creditCard shouldBe card.id
        saved.captured.metadata.purchaseDateTime shouldBe purchase
        saved.captured.time shouldBe Instant.parse("2024-03-10T10:00:00Z")
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
    fun `a record in another currency with no rate isn't taken one for one, as it can't be converted`() = runTest {
        // given
        coEvery { exchangeUseCase.convert(any<PositiveValue>(), any()) } returns None

        // when, then
        shouldThrow<NoLoanExchangeRate> {
            useCase.convertedAmount(
                loanAccount = BgnAccount.id,
                record = record(type = LoanRecordType.DECREASE, account = EurAccount.id),
            )
        }
    }

    @Test
    fun `when one record can't be converted, an account change converts none of them`() = runTest {
        // given: the loan moves from BGN to EUR; its records are in BGN, which converts, and USD
        val loan = BorrowedLoan.copy(account = EurAccount.id)
        val inLev = record(type = LoanRecordType.DECREASE, account = BgnAccount.id)
        val inDollars = record(type = LoanRecordType.DECREASE, account = UsdAccount.id)
        coEvery { loanRepository.findRecordsByLoanId(loan.id) } returns listOf(inLev, inDollars)
        coEvery { exchangeUseCase.convert(PositiveValue(inLev.amount, Bgn), to = Eur) } returns
            Some(PositiveValue(PositiveDouble.unsafe(51.0), Eur))
        coEvery { exchangeUseCase.convert(PositiveValue(inDollars.amount, Usd), to = Eur) } returns None

        // when
        shouldThrow<NoLoanExchangeRate> { useCase.recalculateRecords(loan, oldAccount = BgnAccount.id) }

        // then
        coVerify(exactly = 0) { loanRepository.saveRecord(any()) }
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

    @Test
    fun `changing a loan's type flips the records' existing transactions`() = runTest {
        // given: a borrowed loan, repaid with an expense, is changed to a lent one
        val repaid = record(type = LoanRecordType.DECREASE, account = BgnAccount.id)
        val noTransaction = record(type = LoanRecordType.DECREASE, account = BgnAccount.id)
        coEvery { loanRepository.findRecordsByLoanId(BorrowedLoan.id) } returns
            listOf(repaid, noTransaction)
        coEvery { transactionRepository.findLoanRecordTransaction(repaid.id.value) } returns
            mockk<Expense>(relaxed = true)
        val lent = BorrowedLoan.copy(type = LoanType.LEND)

        // when
        useCase.syncRecordTransactions(lent)

        // then: being paid back on a lent loan is income; a record without one gets none
        val saved = slot<Transaction>()
        coVerify(exactly = 1) { transactionRepository.save(capture(saved)) }
        saved.captured.shouldBeInstanceOf<Income>().metadata.loanRecordId shouldBe repaid.id.value
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
        val Usd = AssetCode.unsafe("USD")

        val BgnAccount = account("Bank", Bgn)
        val EurAccount = account("Revolut", Eur)
        val UsdAccount = account("Dollars", Usd)

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
            type = AccountType.OTHER,
            archived = false,
            orderNum = 0.0,
        )
    }
}
