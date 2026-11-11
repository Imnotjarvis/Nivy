package com.nivy.wallet.stats

import arrow.core.None
import com.nivy.balance.usecase.MonthDetailUseCase
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceDetails
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.reports.trends.TrendsRange
import com.nivy.reports.usecase.TrendsUseCase
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import io.kotest.matchers.doubles.plusOrMinus
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

/**
 * The net worth screen's month and Trends show tiles under the same names. The two screens are
 * features that can't see each other, so this is where the same month is put through both: what
 * one counts as a typical purchase, a day without spending, a small purchase or a fixed cost, the
 * other does too.
 */
class MonthAndTrendsAgreeTest {
    // October 2026, tracked from the 3rd: what's bought by choice, a bill and a subscription that
    // planned payments paid, a loan's repayment, and a purchase in an account kept out of the balance.
    private val transactions = listOf(
        income(day = 3, amount = 2_000.0),
        expense(day = 3, amount = 12.0),
        expense(day = 5, amount = 550.0, recurring = true),
        expense(day = 8, amount = 13.0, recurring = true),
        expense(day = 10, amount = 200.0, loan = true),
        expense(day = 12, amount = 80.0),
        expense(day = 12, amount = 3.0),
        expense(day = 20, amount = 45.5),
        expense(day = 21, amount = 99.0, account = Hidden),
    )

    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAllBetween(any(), any()) } answers {
            val (from, to) = firstArg<Instant>() to secondArg<Instant>()
            transactions.filter { it.time in from..to }
        }
        coEvery { findAllDueToBetween(any(), any()) } returns emptyList()
        coEvery { findFirstPaidTime() } returns transactions.minOf { it.time }
    }
    private val accountRepository = mockk<AccountRepository> {
        coEvery { findAll() } returns listOf(Bank, Hidden)
    }
    private val categoryRepository = mockk<CategoryRepository> {
        coEvery { findAll() } returns emptyList()
    }

    // The balances aren't what the tiles count: every account holds nothing.
    private val accountBalanceUseCase = mockk<AccountBalanceUseCase> {
        coEvery { balance(any(), any()) } answers { AccountBalanceDetails(firstArg(), BigDecimal.ZERO) }
    }
    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { convert(any<PositiveValue>(), any()) } returns None
    }

    private val month = MonthDetailUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        budgetRepository = mockk<BudgetRepository> { coEvery { findAll() } returns emptyList() },
        walletBalanceUseCase = mockk<WalletBalanceUseCase> {
            coEvery { balance(any(), any()) } returns BigDecimal.ZERO
        },
        accountBalanceUseCase = accountBalanceUseCase,
        exchangeUseCase = exchangeUseCase,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        dispatchers = TestDispatchersProvider,
    )
    private val trends = TrendsUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        accountBalanceUseCase = accountBalanceUseCase,
        exchangeUseCase = exchangeUseCase,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `a month's tiles count the same purchases and days on the net worth screen and on Trends`() = runTest {
        // when
        val onMonth = month.detail(October, startDayOfMonth = 1, baseCurrency = "EUR").habits!!
        val onTrends = trends.trends(TrendsRange.ThreeMonths, startDayOfMonth = 1, baseCurrency = Eur)
            .months.single { it.period == October }

        // then the four bought by choice are the purchases: 12, 80, 3 and 45.50
        onMonth.purchases shouldBe 4
        onTrends.habits.purchases shouldBe 4
        // Typical purchase
        onMonth.median shouldBe 28.75
        onTrends.habits.typical shouldBe 28.75
        // No-spend days: of the 29 from the 3rd, all but the 3rd, the 12th and the 20th
        onMonth.trackedDays shouldBe 29
        onTrends.habits.days shouldBe 29
        onMonth.noSpendDays shouldBe 26
        onTrends.habits.noSpendDays shouldBe 26
        // Small purchases: the 12 and the 3, not the subscription's 13
        onMonth.small.count shouldBe 2
        onMonth.small.share * onMonth.chosenSpent shouldBe (15.0 plusOrMinus Cent)
        onTrends.habits.small shouldBe 15.0
        // Fixed and flexible: the bill and the subscription against the purchases by choice; the
        // loan's repayment is neither
        onMonth.fixed.amount shouldBe 563.0
        onTrends.fixed shouldBe 563.0
        onMonth.flexible.amount shouldBe 140.5
        onTrends.flexible shouldBe 140.5
    }

    private companion object {
        const val Cent = 0.01
        val Eur = AssetCode.unsafe("EUR")
        val October = TimePeriod(month = Month.fromMonthValue(10), year = 2026)

        val Bank = account("Bank", n = 1, inBalance = true)
        val Hidden = account("Savings box", n = 2, inBalance = false)

        /** November 15, 2026, noon, in UTC: October is over. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        fun account(name: String, n: Long, inBalance: Boolean) = Account(
            id = AccountId(UUID(1L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            asset = Eur,
            color = ColorInt(0),
            icon = null,
            includeInBalance = inBalance,
            type = AccountType.BANK,
            archived = false,
            orderNum = n.toDouble(),
        )

        fun october(day: Int): Instant = LocalDateTime.of(2026, 10, day, 13, 0).toInstant(ZoneOffset.UTC)

        fun expense(
            day: Int,
            amount: Double,
            account: Account = Bank,
            recurring: Boolean = false,
            loan: Boolean = false,
        ) = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = october(day),
            settled = true,
            metadata = Metadata.copy(
                recurringRuleId = if (recurring) UUID(8L, 8L) else null,
                loanId = if (loan) UUID(7L, 7L) else null,
            ),
            value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
            account = account.id,
        )

        fun income(day: Int, amount: Double) = Income(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = october(day),
            settled = true,
            metadata = Metadata,
            value = PositiveValue(PositiveDouble.unsafe(amount), Eur),
            account = Bank.id,
        )

        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )
    }
}
