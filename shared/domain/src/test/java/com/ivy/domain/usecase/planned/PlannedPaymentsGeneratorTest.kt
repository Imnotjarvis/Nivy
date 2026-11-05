package com.ivy.domain.usecase.planned

import com.ivy.base.model.AccountType
import com.ivy.base.model.IntervalType
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCardId
import com.ivy.data.model.Expense
import com.ivy.data.model.PlannedPaymentInterval
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.PositiveInt
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
    }
    private val accountRepository = mockk<AccountRepository> {
        coEvery { findById(Cash.id) } returns Cash
    }
    private val timeProvider = mockk<TimeProvider> {
        every { utcNow() } returns date(2026, 1, 1)
        every { getZoneId() } returns ZoneOffset.UTC
    }
    private val generator = PlannedPaymentsGenerator(transactionRepository, accountRepository, timeProvider)

    @Test
    fun `monthly dates keep the start date's day after a short month`() = runTest {
        generator.generate(monthlyFrom(date(2026, 1, 31)))

        unpaidDates().take(4) shouldBe listOf(
            date(2026, 1, 31), date(2026, 2, 28), date(2026, 3, 31), date(2026, 4, 30)
        )
    }

    @Test
    fun `an edit brings back neither a skipped nor a paid occurrence`() = runTest {
        // given: January skipped (deleted), February paid
        val rule = monthlyFrom(date(2026, 1, 1))
        generator.generate(rule)
        stored.removeAll { it.time == date(2026, 1, 1) }
        pay(date(2026, 2, 1))

        // when: the rule is edited, which regenerates it
        generator.generate(rule)

        // then
        unpaidDates().first() shouldBe date(2026, 3, 1)
    }

    @Test
    fun `topping up adds occurrences after the latest one only`() = runTest {
        // given: a daily rule whose 72 occurrences were all handled, the first one skipped
        val rule = dailyFrom(date(2026, 1, 1))
        generator.generate(rule)
        val first = stored.minOf { it.time }
        stored.removeAll { it.time == first }
        stored.map { it.time }.forEach(::pay)

        // when
        generator.topUp(rule)

        // then: new ones start the day after the last paid one, and the skipped one stays gone
        unpaidDates().first() shouldBe date(2026, 3, 14)
        unpaidDates().size shouldBe 72
    }

    @Test
    fun `a planned expense paid with a card is planned on that card`() = runTest {
        val card = CreditCardId(UUID.randomUUID())

        generator.generate(monthlyFrom(date(2026, 1, 1)).copy(creditCard = card))

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

    private fun rule(start: Instant, type: IntervalType) = PlannedPaymentRule(
        id = PlannedPaymentRuleId(UUID.randomUUID()),
        type = TransactionType.EXPENSE,
        account = Cash.id,
        amount = PositiveDouble.unsafe(10.0),
        category = null,
        title = null,
        description = null,
        startDate = start,
        interval = PlannedPaymentInterval(PositiveInt.unsafe(1), type),
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

        fun date(year: Int, month: Int, day: Int): Instant =
            LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant()
    }
}
