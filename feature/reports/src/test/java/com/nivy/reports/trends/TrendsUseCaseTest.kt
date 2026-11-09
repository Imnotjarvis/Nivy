package com.nivy.reports.trends

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceDetails
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class TrendsUseCaseTest {
    private val bank = account("Bank", includeInBalance = true)
    private val savings = account("Savings", includeInBalance = false)

    private var firstPaid: Instant? = Instant.parse("2025-06-01T10:00:00Z")
    private val transactions = listOf(
        income(bank, 1_000.0, "2026-11-05T10:00:00Z"),
        income(savings, 500.0, "2026-11-06T10:00:00Z"),
        expense(bank, 200.0, "2026-10-20T10:00:00Z"),
        transfer(bank, savings, 400.0, "2026-10-06T10:00:00Z"),
        expense(bank, 80.0, "2026-11-10T10:00:00Z", settled = false),
    )

    // What a balance is asked at; the bank answers with the month's number, the savings with 100.
    private val asked = mutableListOf<Pair<Account, Instant>>()

    private val useCase = TrendsUseCase(
        transactionRepository = mockk<TransactionRepository> {
            coEvery { findFirstPaidTime() } answers { firstPaid }
            coEvery { findAllBetween(any(), any()) } answers {
                val (from, to) = firstArg<Instant>() to secondArg<Instant>()
                transactions.filter { it.time in from..to }
            }
        },
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns listOf(bank, savings) },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
        accountBalanceUseCase = mockk<AccountBalanceUseCase> {
            coEvery { balance(any(), any()) } answers {
                val account = firstArg<Account>()
                val at = secondArg<ClosedTimeRange>().to
                asked += account to at
                val balance = if (account == bank) at.atOffset(ZoneOffset.UTC).monthValue.toDouble() else 100.0
                AccountBalanceDetails(account, BigDecimal.valueOf(balance))
            }
        },
        exchangeUseCase = mockk<ExchangeUseCase>(),
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `net worth counts every account, at each month's end and now for the current month`() = runTest {
        // when
        val trends = useCase.trends(TrendsRange.Year, startDayOfMonth = 1, baseCurrency = Eur)

        // then: December 2025 to November 2026
        trends.months.map { it.netWorth } shouldBe (listOf(12.0) + (1..11).map { it.toDouble() }).map { it + 100.0 }
        asked.map { it.first }.toSet() shouldBe setOf(bank, savings)
        asked.first().second.atOffset(ZoneOffset.UTC).toLocalDate() shouldBe LocalDate.of(2025, 12, 31)
        asked.last().second shouldBe FixedTime.utcNow()
    }

    @Test
    fun `income and expenses count in their month, of the accounts included in the balance`() = runTest {
        // when
        val months = useCase.trends(TrendsRange.ThreeMonths, startDayOfMonth = 1, baseCurrency = Eur).months

        // then: September to November
        months.map { it.income } shouldBe listOf(0.0, 0.0, 1_000.0)
        months.map { it.expense } shouldBe listOf(0.0, 200.0, 0.0)
    }

    @Test
    fun `transfers aren't income or expenses, and planned payments don't count until paid`() = runTest {
        // when
        val months = useCase.trends(TrendsRange.ThreeMonths, startDayOfMonth = 1, baseCurrency = Eur).months

        // then: the 400 moved to the savings in October, and the 80 due on November 10th, aren't spent.
        months[1].expense shouldBe 200.0
        months[2].expense shouldBe 0.0
    }

    @Test
    fun `all goes back to the first paid transaction's month`() = runTest {
        // given
        firstPaid = Instant.parse("2026-09-03T10:00:00Z")

        // when
        val months = useCase.trends(TrendsRange.All, startDayOfMonth = 1, baseCurrency = Eur).months

        // then
        months.map { it.period.month?.monthValue } shouldBe listOf(9, 10, 11)
    }

    @Test
    fun `days before the first paid transaction aren't days without spending`() = runTest {
        // given
        firstPaid = Instant.parse("2026-10-06T10:00:00Z")

        // when
        val months = useCase.trends(TrendsRange.ThreeMonths, startDayOfMonth = 1, baseCurrency = Eur).months

        // then: September before the app was used, October from the 6th, November to the 15th.
        months.map { it.habits.days } shouldBe listOf(0, 26, 15)
        months.map { it.full } shouldBe listOf(false, false, false)
    }

    private fun income(account: Account, amount: Double, time: String): Transaction = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = eur(amount),
        account = account.id,
    )

    private fun expense(account: Account, amount: Double, time: String, settled: Boolean = true): Transaction = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = settled,
        metadata = Metadata,
        tags = emptyList(),
        value = eur(amount),
        account = account.id,
    )

    private fun transfer(from: Account, to: Account, amount: Double, time: String): Transaction = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        fromAccount = from.id,
        fromValue = eur(amount),
        toAccount = to.id,
        toValue = eur(amount),
    )

    private fun eur(amount: Double) = PositiveValue(PositiveDouble.unsafe(amount), Eur)

    private fun account(name: String, includeInBalance: Boolean) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = Eur,
        color = ColorInt(0),
        icon = null,
        includeInBalance = includeInBalance,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        /** November 15, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
