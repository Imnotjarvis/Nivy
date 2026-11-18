package com.nivy.search

import arrow.core.Some
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.ui.testing.ComposeViewModelTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class SearchViewModelTest : ComposeViewModelTest() {
    private val coffee = expense(title = "Morning coffee")
    private val rent = expense(title = "Rent")

    // Today is November 18, 2026: a salary paid in October, the plan's next two, and one missed.
    private val paidSalary = income(title = "Salary", time = "2026-10-27T09:00:00Z", settled = true)
    private val laterSalary = income(title = "Salary", time = "2026-12-27T09:00:00Z", settled = false)
    private val nextSalary = income(title = "Salary", time = "2026-11-27T09:00:00Z", settled = false)
    private val missedSalary = income(title = "Salary", time = "2026-09-27T09:00:00Z", settled = false)

    // Lunches paid, one in dollars, a refund, a transfer for them, and one still to pay.
    private val lunch = expense(title = "Lunch", amount = 12.5)
    private val lunchInNewYork = expense(title = "Lunch in New York", amount = 20.0, currency = Usd)
    private val lunchRefund = income(
        title = "Lunch refund",
        time = "2026-11-02T13:00:00Z",
        settled = true,
        amount = 7.5,
    )
    private val lunchFund = transfer(title = "Lunch fund", amount = 100.0)
    private val teamLunch = expense(title = "Team lunch", amount = 40.0, settled = false)

    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAll() } returns listOf(
            coffee, rent, paidSalary, laterSalary, nextSalary, missedSalary,
            lunch, lunchInNewYork, lunchRefund, lunchFund, teamLunch,
        )
    }
    private val dueTransactionsUseCase = mockk<DueTransactionsUseCase>(relaxed = true)
    private val searched = slot<List<Transaction>>()

    // What the screen lists for what's found, whatever it is.
    private val results = listOf(TransactionHistoryDateDivider(LocalDate.of(2023, 11, 1), 0.0, 0.0))

    private val viewModel = SearchViewModel(
        transactionsWithDateDividersUseCase = mockk<TransactionsWithDateDividersUseCase> {
            coEvery { withDateDividers(any(), capture(searched)) } returns results
        },
        transactionsIncomeExpenseUseCase = TransactionsIncomeExpenseUseCase(
            // A dollar is 0.9 euros; the base currency's amounts count as they are.
            exchangeUseCase = mockk<ExchangeUseCase> {
                coEvery { convert(value(1.0, Usd), Eur) } returns Some(value(0.9, Eur))
            },
            dispatchers = TestDispatchersProvider,
        ),
        dueTransactionsUseCase = dueTransactionsUseCase,
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns persistentListOf() },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        currencyRepository = mockk<CurrencyRepository> {
            coEvery { getBaseCurrency() } returns Eur
        },
        transactionRepository = transactionRepository,
        userWrites = UserWrites(mockk(relaxed = true)),
        features = mockk(relaxed = true),
        timeProvider = November18,
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `nothing is listed before anything is typed`() {
        viewModel.runTest {
            transactions.shouldBeEmpty()
        }
        coVerify(exactly = 0) { transactionRepository.findAll() }
    }

    @Test
    fun `a query lists the transactions it matches`() {
        viewModel.runTest(events = listOf(SearchEvent.Search("coffee"))) {
            searchQuery shouldBe "coffee"
            transactions shouldBe results
        }
        searched.captured shouldBe listOf(coffee)
    }

    @Test
    fun `clearing the query lists nothing again`() {
        viewModel.runTest(events = listOf(SearchEvent.Search("coffee"), SearchEvent.Search(""))) {
            searchQuery shouldBe ""
            transactions.shouldBeEmpty()
        }
    }

    @Test
    fun `planned payments found are listed apart, from today on as upcoming and earlier as overdue`() {
        // when
        viewModel.runTest(events = listOf(SearchEvent.Search("salary"))) {
            // then
            upcoming.trns shouldBe listOf(nextSalary, laterSalary)
            upcoming.stats.income.toDouble() shouldBe 3_600.0
            overdue.trns shouldBe listOf(missedSalary)
            overdue.stats.income.toDouble() shouldBe 1_800.0
        }
        searched.captured shouldBe listOf(paidSalary)
    }

    @Test
    fun `the totals count what was paid, in the base currency, without transfers or what's still to pay`() {
        // when
        viewModel.runTest(events = listOf(SearchEvent.Search("lunch"))) {
            // then: 12.50 EUR and 20 USD at 0.9, and the refund.
            expenses shouldBe 30.5
            income shouldBe 7.5
        }
    }

    @Test
    fun `getting a planned income found gets it, and the search runs again`() {
        // when
        viewModel.runTest(events = listOf(SearchEvent.Search("salary"), SearchEvent.PayOrGet(nextSalary))) {}

        // then
        coVerify(exactly = 1) { dueTransactionsUseCase.pay(nextSalary) }
        coVerify(exactly = 2) { transactionRepository.findAll() }
    }

    @Test
    fun `skipping planned payments found skips them, and the search runs again`() {
        // when
        val skipped = listOf(nextSalary, laterSalary)
        viewModel.runTest(events = listOf(SearchEvent.Search("salary"), SearchEvent.Skip(skipped))) {}

        // then
        coVerify(exactly = 1) { dueTransactionsUseCase.skip(skipped) }
        coVerify(exactly = 2) { transactionRepository.findAll() }
    }

    private fun expense(
        title: String,
        amount: Double = 10.0,
        currency: AssetCode = Eur,
        settled: Boolean = true,
    ) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(title),
        description = null,
        category = null,
        time = Instant.parse("2023-11-01T10:00:00Z"),
        settled = settled,
        metadata = Metadata,
        value = value(amount, currency),
        account = AccountId(UUID.randomUUID()),
    )

    private fun income(title: String, time: String, settled: Boolean, amount: Double = 1_800.0) = Income(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(title),
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = settled,
        metadata = Metadata,
        value = value(amount, Eur),
        account = AccountId(UUID.randomUUID()),
    )

    private fun transfer(title: String, amount: Double) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe(title),
        description = null,
        category = null,
        time = Instant.parse("2026-11-03T08:00:00Z"),
        settled = true,
        metadata = Metadata,
        fromAccount = AccountId(UUID.randomUUID()),
        fromValue = value(amount, Eur),
        toAccount = AccountId(UUID.randomUUID()),
        toValue = value(amount, Eur),
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        fun value(amount: Double, asset: AssetCode) = PositiveValue(PositiveDouble.unsafe(amount), asset)

        /** November 18, 2026, at noon, in UTC. */
        val November18 = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 18, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
