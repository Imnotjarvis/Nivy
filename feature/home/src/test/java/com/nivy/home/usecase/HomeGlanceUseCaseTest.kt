package com.nivy.home.usecase

import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
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

class HomeGlanceUseCaseTest {
    private val overdueTransactions = mockk<OverdueTransactionsUseCase> {
        coEvery { overdue(any(), any()) } returns OverdueTransactions(IncomeExpensePair.zero(), emptyList())
    }
    private val upcomingTransactions = mockk<UpcomingTransactionsUseCase> {
        coEvery { upcoming(any(), any()) } returns UpcomingTransactions(IncomeExpensePair.zero(), emptyList())
    }
    private val walletIncomeExpense = mockk<WalletIncomeExpenseUseCase> {
        coEvery { incomeExpense(any(), any(), any()) } returns IncomeExpensePair.zero()
    }
    private val useCase = HomeGlanceUseCase(
        overdueTransactionsUseCase = overdueTransactions,
        upcomingTransactionsUseCase = upcomingTransactions,
        walletIncomeExpenseUseCase = walletIncomeExpense,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
    )

    @Test
    fun `the glance counts the payments overdue until today`() = runTest {
        // given
        coEvery { overdueTransactions.overdue(toRange = EndOfToday, baseCurrency = "EUR") } returns
            OverdueTransactions(IncomeExpensePair.zero(), listOf(expense(dueOn = 1), expense(dueOn = 5)))

        // when
        val glance = useCase.glance(baseCurrency = "EUR", accounts = emptyList(), categories = emptyList())

        // then
        glance.overdue shouldBe 2
    }

    @Test
    fun `what was spent today is the expense total, as the month's is counted, over today`() = runTest {
        // given
        coEvery {
            walletIncomeExpense.incomeExpense(
                baseCurrency = "EUR",
                accounts = any(),
                range = ClosedTimeRange(from = StartOfToday, to = EndOfToday),
            )
        } returns IncomeExpensePair(income = BigDecimal("100"), expense = BigDecimal("12.40"))

        // when
        val glance = useCase.glance(baseCurrency = "EUR", accounts = emptyList(), categories = emptyList())

        // then
        glance.spentToday shouldBe BigDecimal("12.40")
        glance.baseCurrency shouldBe "EUR"
    }

    @Test
    fun `the next payment is the first one due from today to a week after`() = runTest {
        // given
        coEvery {
            upcomingTransactions.upcoming(
                range = ClosedTimeRange(from = StartOfToday, to = EndOfWeekAfter),
                baseCurrency = "EUR",
            )
        } returns UpcomingTransactions(
            IncomeExpensePair.zero(),
            listOf(expense(dueOn = 14, title = "Gym"), expense(dueOn = 12, title = "Rent", amount = 850.0)),
        )

        // when
        val glance = useCase.glance(baseCurrency = "EUR", accounts = emptyList(), categories = emptyList())

        // then
        glance.nextPayment shouldBe NextPayment(
            name = "Rent",
            type = TransactionType.EXPENSE,
            amount = 850.0,
            currency = "EUR",
            daysLeft = 3,
        )
    }

    @Test
    fun `a next payment without a title takes its category's name, and keeps its own currency`() = runTest {
        // given
        val salary = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Salary"),
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
        )
        coEvery { upcomingTransactions.upcoming(any(), any()) } returns UpcomingTransactions(
            IncomeExpensePair.zero(),
            listOf(income(dueOn = 9, category = salary.id, amount = 2400.0, currency = "USD")),
        )

        // when
        val glance = useCase.glance(baseCurrency = "EUR", accounts = emptyList(), categories = listOf(salary))

        // then
        glance.nextPayment shouldBe NextPayment(
            name = "Salary",
            type = TransactionType.INCOME,
            amount = 2400.0,
            currency = "USD",
            daysLeft = 0,
        )
    }

    @Test
    fun `with nothing due within a week, there's no next payment`() = runTest {
        // given nothing upcoming

        // when
        val glance = useCase.glance(baseCurrency = "EUR", accounts = emptyList(), categories = emptyList())

        // then
        glance.nextPayment shouldBe null
    }

    private fun expense(
        dueOn: Int,
        title: String? = null,
        amount: Double = 10.0,
    ) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = title?.let(NotBlankTrimmedString::unsafe),
        description = null,
        category = null,
        time = november(dueOn),
        settled = false,
        metadata = Metadata,
        value = PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe("EUR")),
        account = AccountId(UUID.randomUUID()),
    )

    private fun income(
        dueOn: Int,
        category: CategoryId,
        amount: Double,
        currency: String,
    ) = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = category,
        time = november(dueOn),
        settled = false,
        metadata = Metadata,
        value = PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe(currency)),
        account = AccountId(UUID.randomUUID()),
    )

    private companion object {
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        /** A November 2026 day, 9 in the morning, in UTC. */
        fun november(day: Int): Instant = LocalDateTime.of(2026, 11, day, 9, 0).toInstant(ZoneOffset.UTC)

        /** November 9, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 9, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
        val StartOfToday: Instant = LocalDate.of(2026, 11, 9).atStartOfDay().toInstant(ZoneOffset.UTC)
        val EndOfToday: Instant = LocalDate.of(2026, 11, 9).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)
        val EndOfWeekAfter: Instant = LocalDate.of(2026, 11, 16).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)
    }
}
