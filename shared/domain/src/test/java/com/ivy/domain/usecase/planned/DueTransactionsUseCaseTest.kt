package com.ivy.domain.usecase.planned

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.IntervalType
import com.ivy.data.model.PlannedPaymentInterval
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.PositiveInt
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.PlannedPaymentRuleRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.transaction.TransactionAmountsUseCase
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
    private val timeProvider = mockk<TimeProvider>()

    private val useCase = DueTransactionsUseCase(
        transactionRepository = transactionRepository,
        plannedPaymentRuleRepository = plannedPaymentRuleRepository,
        currencyRepository = currencyRepository,
        transactionAmountsUseCase = transactionAmountsUseCase,
        timeProvider = timeProvider,
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

        // then
        coVerify(exactly = 0) { plannedPaymentRuleRepository.deleteById(any()) }
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
        ),
        tags = emptyList(),
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
            amount = PositiveDouble.unsafe(42.0),
            category = null,
            title = null,
            description = null,
            startDate = DueTime,
            interval = interval,
        )
    }
}
