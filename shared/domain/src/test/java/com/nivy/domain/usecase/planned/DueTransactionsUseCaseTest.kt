package com.nivy.domain.usecase.planned

import arrow.core.Some
import com.nivy.base.model.IntervalType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.transaction.TransactionAmountsUseCase
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

class DueTransactionsUseCaseTest {
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val plannedPaymentRuleRepository = mockk<PlannedPaymentRuleRepository>(relaxed = true)
    private val currencyRepository = mockk<CurrencyRepository>()
    private val transactionAmountsUseCase = mockk<TransactionAmountsUseCase>()
    private val plannedPaymentsGenerator = mockk<PlannedPaymentsGenerator>(relaxed = true)
    private val timeProvider = mockk<TimeProvider>()
    private val creditCardChargeUseCase = mockk<CreditCardChargeUseCase> {
        coEvery { dated(any()) } answers { firstArg() }
    }

    // The day's rate from euros to dollars: 1.25.
    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { convert(any(), AssetCode.unsafe("USD")) } answers {
            val amount = firstArg<PositiveValue>().amount.value * 1.25
            Some(PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe("USD")))
        }
    }

    // The database transaction's block runs as it is; inTransaction says when.
    private var inTransaction = false
    private val databaseTransaction = object : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R {
            inTransaction = true
            return try {
                block()
            } finally {
                inTransaction = false
            }
        }
    }

    private val useCase = DueTransactionsUseCase(
        transactionRepository = transactionRepository,
        plannedPaymentRuleRepository = plannedPaymentRuleRepository,
        currencyRepository = currencyRepository,
        transactionAmountsUseCase = transactionAmountsUseCase,
        plannedPaymentsGenerator = plannedPaymentsGenerator,
        creditCardChargeUseCase = creditCardChargeUseCase,
        exchangeUseCase = exchangeUseCase,
        timeProvider = timeProvider,
        databaseTransaction = databaseTransaction,
    )

    init {
        coEvery { timeProvider.utcNow() } returns Now
        coEvery { plannedPaymentRuleRepository.findById(OneTimeRule.id) } returns OneTimeRule
        coEvery { plannedPaymentRuleRepository.findById(MonthlyRule.id) } returns MonthlyRule
    }

    @Test
    fun `paying settles the transaction now, paid for its due time`() = runTest {
        // given
        val due = expense(rule = MonthlyRule.id, settled = false)

        // when
        val paid = useCase.pay(due)

        // then
        val saved = slot<Transaction>()
        coVerify { transactionRepository.save(capture(saved)) }
        val expense = saved.captured.shouldBeInstanceOf<Expense>()
        expense.settled shouldBe true
        expense.time shouldBe Now
        expense.metadata.paidForDateTime shouldBe DueTime
        paid shouldBe expense
    }

    @Test
    fun `paying saves the payment and the rule's next occurrences in one database transaction`() = runTest {
        // given
        val writes = mutableListOf<Boolean>()
        coEvery { transactionRepository.save(any()) } answers { writes += inTransaction }
        coEvery { plannedPaymentsGenerator.topUp(MonthlyRule) } answers { writes += inTransaction }

        // when
        useCase.pay(expense(rule = MonthlyRule.id, settled = false))

        // then
        writes shouldBe listOf(true, true)
    }

    @Test
    fun `skipping deletes the payments and a one-time rule in one database transaction`() = runTest {
        // given
        val writes = mutableListOf<Boolean>()
        coEvery { transactionRepository.deleteById(any()) } answers { writes += inTransaction }
        coEvery { plannedPaymentRuleRepository.deleteById(OneTimeRule.id) } answers { writes += inTransaction }

        // when
        useCase.skip(
            listOf(expense(rule = MonthlyRule.id, settled = false), expense(rule = OneTimeRule.id, settled = false))
        )

        // then
        writes shouldBe listOf(true, true, true)
    }

    @Test
    fun `paying a one-time rule's transaction deletes the rule`() = runTest {
        // when
        useCase.pay(expense(rule = OneTimeRule.id, settled = false))

        // then
        coVerify { plannedPaymentRuleRepository.deleteById(OneTimeRule.id) }
    }

    @Test
    fun `paying keeps a recurring rule`() = runTest {
        // when
        useCase.pay(expense(rule = MonthlyRule.id, settled = false))

        // then: it gets more occurrences instead
        coVerify(exactly = 0) { plannedPaymentRuleRepository.deleteById(any()) }
        coVerify { plannedPaymentsGenerator.topUp(MonthlyRule) }
    }

    @Test
    fun `a settled transaction can't be paid again`() = runTest {
        // when
        val paid = useCase.pay(expense(rule = MonthlyRule.id, settled = true))

        // then
        paid shouldBe null
        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `skipping deletes only the due transactions`() = runTest {
        // given
        val due = expense(rule = OneTimeRule.id, settled = false)
        val settled = expense(rule = MonthlyRule.id, settled = true)

        // when
        useCase.skip(listOf(due, settled))

        // then
        coVerify { transactionRepository.deleteById(due.id) }
        coVerify(exactly = 0) { transactionRepository.deleteById(settled.id) }
        coVerify { plannedPaymentRuleRepository.deleteById(OneTimeRule.id) }
    }

    @Test
    fun `a transfer between currencies is paid at the day's rate`() = runTest {
        // given: planned at 1.10, paid when the rate is 1.25
        val due = Transfer(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = DueTime,
            settled = false,
            metadata = expense(rule = MonthlyRule.id, settled = false).metadata,
            fromAccount = Account,
            fromValue = PositiveValue(PositiveDouble.unsafe(100.0), AssetCode.unsafe("EUR")),
            toAccount = AccountId(UUID.randomUUID()),
            toValue = PositiveValue(PositiveDouble.unsafe(110.0), Usd),
        )

        // when
        val paid = useCase.pay(due)

        // then
        paid.shouldBeInstanceOf<Transfer>().toValue shouldBe PositiveValue(PositiveDouble.unsafe(125.0), Usd)
        paid.fromValue shouldBe due.fromValue
    }

    @Test
    fun `the due balance is in the base currency`() = runTest {
        // given
        val due = listOf(expense(rule = MonthlyRule.id, settled = false))
        coEvery { transactionRepository.findAllDueToBetween(DueTime, Now) } returns due
        coEvery { currencyRepository.getBaseCurrency() } returns Usd
        coEvery { transactionAmountsUseCase.balance(due, Usd) } returns -42.0

        // when
        val balance = useCase.dueBalance(from = DueTime, to = Now)

        // then
        balance shouldBe -42.0
    }

    private fun expense(rule: PlannedPaymentRuleId, settled: Boolean): Expense = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = DueTime,
        settled = settled,
        metadata = TransactionMetadata(
            recurringRuleId = rule.value,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        value = PositiveValue(PositiveDouble.unsafe(42.0), Usd),
        account = Account,
    )

    private companion object {
        val Usd = AssetCode.unsafe("USD")
        val Account = AccountId(UUID.randomUUID())
        val DueTime: Instant = Instant.parse("2026-10-01T09:00:00Z")
        val Now: Instant = Instant.parse("2026-10-02T10:00:00Z")
        val OneTimeRule = rule(interval = null)
        val MonthlyRule = rule(
            interval = PlannedPaymentInterval(PositiveInt.unsafe(1), IntervalType.MONTH)
        )

        fun rule(interval: PlannedPaymentInterval?) = PlannedPaymentRule(
            id = PlannedPaymentRuleId(UUID.randomUUID()),
            type = TransactionType.EXPENSE,
            account = Account,
            toAccount = null,
            amount = PositiveDouble.unsafe(42.0),
            category = null,
            title = null,
            description = null,
            startDate = DueTime,
            interval = interval,
            endDate = null,
            creditCard = null,
        )
    }
}
