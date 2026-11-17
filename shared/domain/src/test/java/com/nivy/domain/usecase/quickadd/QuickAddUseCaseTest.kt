package com.nivy.domain.usecase.quickadd

import arrow.core.None
import arrow.core.Some
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class QuickAddUseCaseTest {
    private val accountRepository = mockk<AccountRepository> {
        coEvery { findById(Bank.id) } returns Bank
        coEvery { findById(Savings.id) } returns Savings
        coEvery { findById(Dollars.id) } returns Dollars
    }
    private val saved = mutableListOf<Transaction>()
    private val transactionRepository = mockk<TransactionRepository>(relaxUnitFun = true) {
        coEvery { save(any()) } answers { saved += firstArg<Transaction>() }
    }

    // Euros stay euros; today's rate to dollars is 1.25.
    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { convert(any(), Eur) } answers { Some(firstArg()) }
        coEvery { convert(any(), Usd) } answers {
            Some(PositiveValue(PositiveDouble.unsafe(firstArg<PositiveValue>().amount.value * 1.25), Usd))
        }
    }

    // Paid with a card, it's charged on the card's charge date, here a week later.
    private val creditCardChargeUseCase = mockk<CreditCardChargeUseCase> {
        coEvery { dated(any()) } answers {
            val transaction = firstArg<Transaction>()
            if (transaction is Expense && transaction.metadata.creditCard != null) {
                transaction.copy(time = ChargeDate, metadata = transaction.metadata.copy(purchaseDateTime = Now))
            } else {
                transaction
            }
        }
    }
    private val timeProvider = mockk<TimeProvider> { every { utcNow() } returns Now }
    private val dataObserver = mockk<DataObserver>(relaxUnitFun = true)

    private val useCase = QuickAddUseCase(
        accountRepository = accountRepository,
        transactionRepository = transactionRepository,
        exchangeUseCase = exchangeUseCase,
        creditCardChargeUseCase = creditCardChargeUseCase,
        timeProvider = timeProvider,
        dataObserver = dataObserver,
    )

    @Test
    fun `an expense is saved paid, made now, of its amount, on its account`() = runTest {
        // when
        val added = useCase.add(Metro)

        // then
        val expense = saved.single().shouldBeInstanceOf<Expense>()
        expense.settled shouldBe true
        expense.time shouldBe Now
        expense.value shouldBe PositiveValue(PositiveDouble.unsafe(2.2), Eur)
        expense.account shouldBe Bank.id
        expense.title shouldBe Metro.title
        expense.category shouldBe Metro.category
        expense.metadata shouldBe TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )
        added shouldBe expense
    }

    @Test
    fun `one without an amount is saved with the amount entered`() = runTest {
        // when
        useCase.add(Lunch, amount = PositiveDouble.unsafe(11.8))

        // then
        saved.single().shouldBeInstanceOf<Expense>().value shouldBe PositiveValue(PositiveDouble.unsafe(11.8), Eur)
    }

    @Test
    fun `one without an amount isn't saved until one is entered`() = runTest {
        // when
        val added = useCase.add(Lunch)

        // then
        added shouldBe null
        saved shouldBe emptyList()
    }

    @Test
    fun `paid with a card, it's saved on the card's charge date`() = runTest {
        // when
        useCase.add(Metro.copy(creditCard = Visa))

        // then
        val expense = saved.single().shouldBeInstanceOf<Expense>()
        expense.time shouldBe ChargeDate
        expense.metadata.purchaseDateTime shouldBe Now
        expense.metadata.creditCard shouldBe Visa
    }

    @Test
    fun `a transfer between two currencies arrives at the day's rate`() = runTest {
        // when
        useCase.add(ToDollars)

        // then
        val transfer = saved.single().shouldBeInstanceOf<Transfer>()
        transfer.fromAccount shouldBe Bank.id
        transfer.fromValue shouldBe PositiveValue(PositiveDouble.unsafe(100.0), Eur)
        transfer.toAccount shouldBe Dollars.id
        transfer.toValue shouldBe PositiveValue(PositiveDouble.unsafe(125.0), Usd)
    }

    @Test
    fun `a transfer between two currencies without a rate isn't saved`() = runTest {
        // given
        coEvery { exchangeUseCase.convert(any(), Usd) } returns None

        // when
        val added = useCase.add(ToDollars)

        // then
        added shouldBe null
        saved shouldBe emptyList()
    }

    @Test
    fun `a transfer in one currency arrives as it left`() = runTest {
        // when
        useCase.add(ToDollars.copy(toAccount = Savings.id))

        // then
        saved.single().shouldBeInstanceOf<Transfer>().toValue shouldBe
            PositiveValue(PositiveDouble.unsafe(100.0), Eur)
    }

    @Test
    fun `Home hears of what was added, once it's saved`() = runTest {
        // when
        useCase.add(Metro)

        // then
        coVerifyOrder {
            transactionRepository.save(any())
            dataObserver.post(DataWriteEvent.TransactionsChanged)
        }
    }

    @Test
    fun `taking it back deletes the transaction, and Home hears of it`() = runTest {
        // given
        val id = TransactionId(UUID.randomUUID())

        // when
        useCase.undo(id)

        // then
        coVerifyOrder {
            transactionRepository.deleteById(id)
            dataObserver.post(DataWriteEvent.TransactionsChanged)
        }
        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }

    private companion object {
        val Now: Instant = Instant.parse("2026-11-17T08:30:00Z")
        val ChargeDate: Instant = Instant.parse("2026-11-24T12:00:00Z")
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")
        val Visa = CreditCardId(UUID.randomUUID())

        val Bank = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Bank"),
            asset = Eur,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.OTHER,
            archived = false,
            orderNum = 0.0,
        )
        val Savings = Bank.copy(id = AccountId(UUID.randomUUID()), name = NotBlankTrimmedString.unsafe("Savings"))
        val Dollars = Bank.copy(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Dollars"),
            asset = Usd,
        )

        val Metro = QuickAdd(
            id = QuickAddId(UUID.randomUUID()),
            type = TransactionType.EXPENSE,
            account = Bank.id,
            toAccount = null,
            amount = PositiveDouble.unsafe(2.2),
            category = CategoryId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("Metro"),
            creditCard = null,
            orderNum = 0.0,
        )
        val Lunch = Metro.copy(
            id = QuickAddId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("Lunch"),
            amount = null,
        )
        val ToDollars = Metro.copy(
            id = QuickAddId(UUID.randomUUID()),
            type = TransactionType.TRANSFER,
            toAccount = Dollars.id,
            amount = PositiveDouble.unsafe(100.0),
            category = null,
            title = NotBlankTrimmedString.unsafe("To dollars"),
        )
    }
}
