package com.nivy.domain.usecase.planned

import android.database.sqlite.SQLiteFullException
import arrow.core.None
import arrow.core.Some
import com.nivy.base.model.AccountType
import com.nivy.base.model.IntervalType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class PlannedPaymentsGeneratorTest {
    /** The rule's transactions, as the database holds them. */
    private val stored = mutableListOf<Transaction>()

    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAllByRecurringRuleId(any()) } answers { stored.toList() }
        coEvery { deletedByRecurringRuleIdAndNoDateTime(any()) } answers { stored.removeAll { !it.settled } }
        coEvery { save(any()) } answers { stored += firstArg<Transaction>() }
        coEvery { saveMany(any()) } answers { stored += firstArg<List<Transaction>>() }
    }

    // Rolls back as the database does: a block cut short leaves the stored transactions as they were.
    private val databaseTransaction = object : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R {
            val before = stored.toList()
            var done = false
            try {
                return block().also { done = true }
            } finally {
                if (!done) {
                    stored.clear()
                    stored += before
                }
            }
        }
    }
    private val accountRepository = mockk<AccountRepository> {
        coEvery { findById(Cash.id) } returns Cash
        coEvery { findById(Savings.id) } returns Savings
    }

    // Removes the rules that end.
    private val ruleRepository = mockk<PlannedPaymentRuleRepository>(relaxed = true)

    // Today's rate from euros to dollars: 1.10.
    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { convert(any(), Usd) } answers {
            Some(PositiveValue(PositiveDouble.unsafe(firstArg<PositiveValue>().amount.value * 1.1), Usd))
        }
    }
    private val timeProvider = mockk<TimeProvider> {
        every { utcNow() } returns date(2026, 1, 1)
        every { getZoneId() } returns ZoneOffset.UTC
    }
    private val generator = PlannedPaymentsGenerator(
        transactionRepository,
        accountRepository,
        ruleRepository,
        exchangeUseCase,
        timeProvider,
        databaseTransaction,
    )

    @Test
    fun `monthly dates keep the start date's day after a short month`() = runTest {
        generator.generate(monthlyFrom(date(2026, 1, 31)), previous = null)

        unpaidDates().take(4) shouldBe listOf(
            date(2026, 1, 31), date(2026, 2, 28), date(2026, 3, 31), date(2026, 4, 30)
        )
    }

    @Test
    fun `an edit brings back neither a skipped nor a paid occurrence`() = runTest {
        // given: January skipped (deleted), February paid
        val rule = monthlyFrom(date(2026, 1, 1))
        generator.generate(rule, previous = null)
        stored.removeAll { it.time == date(2026, 1, 1) }
        pay(date(2026, 2, 1))

        // when: the rule is edited, which regenerates it
        generator.generate(rule, previous = rule)

        // then
        unpaidDates().first() shouldBe date(2026, 3, 1)
    }

    @Test
    fun `an edit doesn't bring back an occurrence skipped after the last paid one`() = runTest {
        // given: January paid, February skipped
        val rule = monthlyFrom(date(2026, 1, 1))
        generator.generate(rule, previous = null)
        pay(date(2026, 1, 1))
        stored.removeAll { it.time == date(2026, 2, 1) }

        // when: its amount is changed
        generator.generate(rule.copy(amount = PositiveDouble.unsafe(12.0)), previous = rule)

        // then
        unpaidDates().take(2) shouldBe listOf(date(2026, 3, 1), date(2026, 4, 1))
    }

    @Test
    fun `an edit doesn't bring back an occurrence skipped ahead of unpaid ones`() = runTest {
        // given: March skipped, January and February still to pay
        val rule = monthlyFrom(date(2026, 1, 1))
        generator.generate(rule, previous = null)
        stored.removeAll { it.time == date(2026, 3, 1) }

        // when
        generator.generate(rule.copy(amount = PositiveDouble.unsafe(12.0)), previous = rule)

        // then
        unpaidDates().take(3) shouldBe listOf(date(2026, 1, 1), date(2026, 2, 1), date(2026, 4, 1))
    }

    @Test
    fun `moved to an earlier start, a rule plans its new first occurrence`() = runTest {
        // given: a rule from February, nothing paid yet
        val rule = monthlyFrom(date(2026, 2, 1))
        generator.generate(rule, previous = null)

        // when: it starts in January instead
        generator.generate(rule.copy(startDate = date(2026, 1, 1)), previous = rule)

        // then
        unpaidDates().first() shouldBe date(2026, 1, 1)
    }

    @Test
    fun `an edit cut short leaves the payments as they were`() = runTest {
        // given: a rule with its payments, and a database that takes only two more
        val rule = monthlyFrom(date(2026, 1, 1))
        generator.generate(rule, previous = null)
        val before = stored.toList()
        var saves = 0
        coEvery { transactionRepository.save(any()) } answers {
            if (++saves > 2) throw SQLiteFullException("database or disk is full")
            stored += firstArg<Transaction>()
        }
        coEvery { transactionRepository.saveMany(any()) } throws SQLiteFullException("database or disk is full")

        // when
        shouldThrow<SQLiteFullException> {
            generator.generate(rule.copy(amount = PositiveDouble.unsafe(12.0)), previous = rule)
        }

        // then
        stored shouldBe before
    }

    @Test
    fun `topping up adds occurrences after the latest one only`() = runTest {
        // given: a daily rule whose 72 occurrences were all handled, the first one skipped
        val rule = dailyFrom(date(2026, 1, 1))
        generator.generate(rule, previous = null)
        val first = stored.minOf { it.time }
        stored.removeAll { it.time == first }
        stored.map { it.time }.forEach(::pay)

        // when
        generator.topUp(rule)

        // then: new ones start the day after the last paid one, and the skipped one stays gone
        unpaidDates().first() shouldBe date(2026, 3, 14)
        unpaidDates().size shouldBe 72
        coVerify(exactly = 0) { ruleRepository.deleteById(any()) }
    }

    @Test
    fun `a rule with an end date plans nothing after its last day`() = runTest {
        // An end at noon: the occurrence at midnight that day is still in.
        generator.generate(monthlyFrom(date(2026, 1, 1)).copy(endDate = noon(2026, 3, 1)), previous = null)

        unpaidDates() shouldBe listOf(date(2026, 1, 1), date(2026, 2, 1), date(2026, 3, 1))
    }

    @Test
    fun `moving the end earlier drops the payments after it, moving it later adds them`() = runTest {
        // given
        val rule = monthlyFrom(date(2026, 1, 1)).copy(endDate = noon(2026, 6, 1))
        generator.generate(rule, previous = null)

        // when
        val earlier = rule.copy(endDate = noon(2026, 2, 1))
        generator.generate(earlier, previous = rule)

        // then
        unpaidDates() shouldBe listOf(date(2026, 1, 1), date(2026, 2, 1))

        // when
        generator.generate(earlier.copy(endDate = noon(2026, 4, 1)), previous = earlier)

        // then
        unpaidDates() shouldBe listOf(date(2026, 1, 1), date(2026, 2, 1), date(2026, 3, 1), date(2026, 4, 1))
    }

    @Test
    fun `a rule paid up to its end is removed once nothing is left to top up`() = runTest {
        // given: all three payments up to its end paid
        val rule = monthlyFrom(date(2026, 1, 1)).copy(endDate = noon(2026, 3, 1))
        generator.generate(rule, previous = null)
        listOf(date(2026, 1, 1), date(2026, 2, 1), date(2026, 3, 1)).forEach(::pay)

        // when
        generator.topUp(rule)

        // then
        unpaidDates() shouldBe emptyList()
        coVerify { ruleRepository.deleteById(rule.id) }
    }

    @Test
    fun `a rule whose last payments to its end were skipped is removed`() = runTest {
        // given: the only payment before its end skipped, as a skip deletes it
        val rule = monthlyFrom(date(2026, 1, 1)).copy(endDate = noon(2026, 1, 15))
        generator.generate(rule, previous = null)
        stored.clear()

        // when
        generator.topUp(rule)

        // then
        coVerify { ruleRepository.deleteById(rule.id) }
    }

    @Test
    fun `an end moved before what's left to pay removes the rule`() = runTest {
        // given: January paid, then the end moved to before February
        val rule = monthlyFrom(date(2026, 1, 1))
        generator.generate(rule, previous = null)
        pay(date(2026, 1, 1))

        // when
        val ended = rule.copy(endDate = noon(2026, 1, 20))
        generator.generate(ended, previous = rule)

        // then
        unpaidDates() shouldBe emptyList()
        coVerify { ruleRepository.deleteById(rule.id) }
    }

    @Test
    fun `a planned transfer moves its amount to its account, what arrives at today's rate`() = runTest {
        generator.generate(transferFrom(date(2026, 1, 1)), previous = null)

        val first = stored.minBy { it.time } as Transfer
        first.fromAccount shouldBe Cash.id
        first.fromValue shouldBe PositiveValue(PositiveDouble.unsafe(10.0), AssetCode.unsafe("EUR"))
        first.toAccount shouldBe Savings.id
        first.toValue.asset shouldBe Usd
        first.toValue.amount.value shouldBe (11.0 plusOrMinus 0.001)
        first.settled shouldBe false
    }

    @Test
    fun `without a rate, a planned transfer's estimate is its own amount`() = runTest {
        coEvery { exchangeUseCase.convert(any(), any()) } returns None

        generator.generate(transferFrom(date(2026, 1, 1)), previous = null)

        (stored.first() as Transfer).toValue shouldBe PositiveValue(PositiveDouble.unsafe(10.0), Usd)
    }

    @Test
    fun `a planned expense paid with a card is planned on that card`() = runTest {
        val card = CreditCardId(UUID.randomUUID())

        generator.generate(monthlyFrom(date(2026, 1, 1)).copy(creditCard = card), previous = null)

        stored.map { it.metadata.creditCard }.toSet() shouldBe setOf(card)
    }

    private fun pay(due: Instant) {
        val index = stored.indexOfFirst { it.time == due && !it.settled }
        val unpaid = stored[index] as Expense
        stored[index] = unpaid.copy(
            settled = true,
            time = date(2026, 6, 1),
            metadata = unpaid.metadata.copy(paidForDateTime = unpaid.time),
        )
    }

    private fun unpaidDates() = stored.filterNot { it.settled }.map { it.time }.sorted()

    private fun monthlyFrom(start: Instant) = rule(start, IntervalType.MONTH)
    private fun dailyFrom(start: Instant) = rule(start, IntervalType.DAY)
    private fun transferFrom(start: Instant) =
        monthlyFrom(start).copy(type = TransactionType.TRANSFER, toAccount = Savings.id)

    private fun rule(start: Instant, type: IntervalType) = PlannedPaymentRule(
        id = PlannedPaymentRuleId(UUID.randomUUID()),
        type = TransactionType.EXPENSE,
        account = Cash.id,
        toAccount = null,
        amount = PositiveDouble.unsafe(10.0),
        category = null,
        title = null,
        description = null,
        startDate = start,
        interval = PlannedPaymentInterval(PositiveInt.unsafe(1), type),
        endDate = null,
        creditCard = null,
    )

    private companion object {
        val Cash = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Cash"),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.OTHER,
            archived = false,
            orderNum = 0.0,
        )

        val Usd = AssetCode.unsafe("USD")
        val Savings = Cash.copy(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Savings"),
            asset = Usd,
        )

        fun date(year: Int, month: Int, day: Int): Instant =
            LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant()

        fun noon(year: Int, month: Int, day: Int): Instant = date(year, month, day).plusSeconds(12 * 3600L)
    }
}
