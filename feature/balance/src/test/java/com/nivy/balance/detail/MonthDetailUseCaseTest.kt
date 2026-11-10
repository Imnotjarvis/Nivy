package com.nivy.balance.detail

import arrow.core.None
import arrow.core.Some
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
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
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class MonthDetailUseCaseTest {
    private val accounts = mutableListOf(Bank, Card, Hidden)
    private val held = mutableMapOf(Bank to 5_000.0, Card to -550.0, Hidden to 300.0)
    private val transactions = mutableListOf(
        // Paid by today, November 9th.
        income(day = 3, amount = 1_650.0, title = "Salary"),
        expense(day = 5, amount = 130.30, title = "Home goods", category = Shopping),
        // Bought on Saturday the 7th in the evening, charged on the 9th.
        expense(day = 9, hour = 10, amount = 20.0, title = "Lidl", account = Card, bought = at(7, 20)),
        expense(day = 4, amount = 99.0, title = "Kept apart", account = Hidden),
        expense(day = 6, amount = 300.0, title = "Loan back", loan = true),
        // October.
        expense(day = 2, month = 10, amount = 200.0, title = "Sofa", category = Shopping),
        expense(day = 5, month = 10, amount = 50.0, title = "Groceries"),
        // Planned: overdue since the 2nd, then three ahead.
        expense(day = 2, amount = 15.0, title = "Parking fine", paid = false),
        expense(day = 10, amount = 35.0, title = "Gym", paid = false),
        income(day = 20, amount = 100.0, title = "Refund", paid = false),
        expense(day = 25, amount = 550.0, title = "Card bill", paid = false),
    )

    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAllBetween(any(), any()) } answers {
            val (from, to) = firstArg<Instant>() to secondArg<Instant>()
            transactions.filter { it.settled && it.time in from..to }
        }
        coEvery { findAllDueToBetween(any(), any()) } answers {
            val (from, to) = firstArg<Instant>() to secondArg<Instant>()
            transactions.filter { !it.settled && it.time in from..to }
        }
        coEvery { findFirstPaidTime() } answers { transactions.filter { it.settled }.minOfOrNull { it.time } }
    }

    // The wallet's balance is 10,000 euros before any of these, plus what was paid by then in
    // the accounts in the balance.
    private val walletBalanceUseCase = mockk<WalletBalanceUseCase> {
        coEvery { balance(any(), any(), any()) } answers {
            val to = secondArg<ClosedTimeRange?>()?.to ?: Instant.MAX
            val included = accounts.filter { it.includeInBalance }.map { it.id }.toSet()
            val paid = transactions.filter { it.settled && !it.time.isAfter(to) && it.account() in included }
            BigDecimal.valueOf(StartingBalance + paid.sumOf { it.signed() })
        }
    }

    private val accountBalanceUseCase = mockk<AccountBalanceUseCase> {
        coEvery { balance(any(), any()) } answers {
            val account = firstArg<Account>()
            AccountBalanceDetails(account, BigDecimal.valueOf(held.getValue(account)))
        }
    }

    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { convert(any<PositiveValue>(), any()) } returns None
    }

    private val useCase = MonthDetailUseCase(
        transactionRepository = transactionRepository,
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } answers { accounts.toList() } },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns listOf(Shopping, Groceries) },
        walletBalanceUseCase = walletBalanceUseCase,
        accountBalanceUseCase = accountBalanceUseCase,
        exchangeUseCase = exchangeUseCase,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `the balance is the wallet's day by day up to today, then steps with what's due`() = runTest {
        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.days.size shouldBe 30
        detail.today shouldBe LocalDate.of(2026, 11, 9)
        detail.actualDays shouldBe 9
        detail.balances[1] shouldBe (9_750.0 plusOrMinus Cent)
        detail.balances[8] shouldBe (10_949.70 plusOrMinus Cent)
        // The fine overdue and the gym on the 10th, the refund on the 20th, the card's bill on the 25th.
        detail.balances[9] shouldBe (10_899.70 plusOrMinus Cent)
        detail.balances[19] shouldBe (10_999.70 plusOrMinus Cent)
        detail.balances[29] shouldBe (10_449.70 plusOrMinus Cent)
        detail.sinceStart!! shouldBe (1_199.70 plusOrMinus Cent)
        detail.lowest!!.day shouldBe LocalDate.of(2026, 11, 25)
        detail.lowest.amount shouldBe (10_449.70 plusOrMinus Cent)
    }

    @Test
    fun `coming up is what's due from today, the soonest first, and the overdue isn't`() = runTest {
        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.comingUp.map { it.title } shouldBe listOf("Gym", "Refund", "Card bill")
        detail.comingUp.map { it.amount } shouldBe listOf(-35.0, 100.0, -550.0)
        detail.comingUp.map { it.type } shouldBe
            listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.EXPENSE)
        detail.comingUp.first().account shouldBe Bank
        detail.moreComingUp shouldBe 0
        detail.plannedDays shouldBe setOf(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 25))
    }

    @Test
    fun `free to spend counts all that went out, and all still due this month`() = runTest {
        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then: 1,650 in and 100 to come, 450.30 out, the loan's money too, and 600 still due.
        val free = detail.freeToSpend!!
        free.spent shouldBe (450.30 plusOrMinus Cent)
        free.left shouldBe (699.70 plusOrMinus Cent)
        free.daysLeft shouldBe 21
        free.perDay shouldBe (699.70 / 21 plusOrMinus Cent)
    }

    @Test
    fun `habits are the purchases of the accounts in the balance, a loan's money left out`() = runTest {
        // when
        val habits = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR").habits!!

        // then
        habits.purchases shouldBe 2
        habits.spent shouldBe (150.30 plusOrMinus Cent)
        habits.savedShare!! shouldBe ((1_650.0 - 450.30) / 1_650.0 plusOrMinus Cent)
        habits.biggest!!.title shouldBe "Home goods"
        habits.biggest.category shouldBe Shopping
        // Of the first nine days of October, 200 went on shopping.
        habits.categories.first { it.category == Shopping }.change!! shouldBe
            ((130.30 - 200.0) / 200.0 plusOrMinus Cent)
    }

    @Test
    fun `a card's purchase counts on the day it's charged, and was made when it was bought`() = runTest {
        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.spentByDay[8] shouldBe (20.0 plusOrMinus Cent)
        detail.habits!!.byWeekday[DayOfWeek.SATURDAY.ordinal] shouldBe (20.0 plusOrMinus Cent)
        detail.habits.timeOfDay!![Evening] shouldBe (20.0 / 150.30 plusOrMinus Cent)
    }

    @Test
    fun `the pace is set against as many days of the month before`() = runTest {
        // when
        val pace = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR").pace!!

        // then
        pace.previousMonth shouldBe Month.fromMonthValue(10)
        pace.comparedTo shouldBe LocalDate.of(2026, 10, 9)
        pace.current.size shouldBe 9
        pace.current[4] shouldBe (130.30 plusOrMinus Cent)
        pace.current.last() shouldBe (150.30 plusOrMinus Cent)
        pace.previous.size shouldBe 31
        pace.previous[8] shouldBe (250.0 plusOrMinus Cent)
    }

    @Test
    fun `what's owned and owed, and how long it lasts at the last months' spending`() = runTest {
        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.holdings.assets shouldBe listOf(Holding(Bank, 5_000.0))
        detail.holdings.debts shouldBe listOf(Holding(Card, -550.0))
        detail.holdings.netWorth shouldBe (4_450.0 plusOrMinus Cent)
        // Only October, of the three months before, had spending.
        detail.runway shouldBe Runway(months = 4_450.0 / 250.0, monthly = 250.0, over = 1)
    }

    @Test
    fun `amounts in another currency count at its rate, and without a rate are left out`() = runTest {
        // given
        val travel = account("Travel", n = 4, currency = "USD")
        accounts += travel
        held[travel] = 100.0
        transactions += expense(day = 8, amount = 50.0, title = "Museum", account = travel)
        coEvery { exchangeUseCase.convert(PositiveValue(PositiveDouble.unsafe(1.0), Usd), Eur) } returns
            Some(PositiveValue(PositiveDouble.unsafe(0.9), Eur))

        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.holdings.assets shouldBe listOf(Holding(Bank, 5_000.0), Holding(travel, 90.0))
        detail.habits!!.spent shouldBe (150.30 + 45.0 plusOrMinus Cent)

        // given no rate
        coEvery { exchangeUseCase.convert(any<PositiveValue>(), any()) } returns None

        // when
        val unrated = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        unrated.holdings.assets shouldBe listOf(Holding(Bank, 5_000.0))
        unrated.habits!!.spent shouldBe (150.30 plusOrMinus Cent)
    }

    @Test
    fun `a month spent only where there's no rate tells no habits`() = runTest {
        // given every purchase this month made in dollars, with no rate to euros
        val travel = account("Travel", n = 4, currency = "USD")
        accounts += travel
        held[travel] = 100.0
        transactions.removeAll { it is Expense && it.settled }
        transactions += expense(day = 3, amount = 12.0, title = "Lunch", account = travel)
        transactions += expense(day = 4, amount = 30.0, title = "Groceries", account = travel)

        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.habits shouldBe null
    }

    @Test
    fun `a month gone by has no today, nothing coming up and nothing left to spend`() = runTest {
        // when
        val detail = useCase.detail(October, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.days.size shouldBe 31
        detail.today.shouldBeNull()
        detail.actualDays shouldBe 31
        detail.balances.last() shouldBe (9_750.0 plusOrMinus Cent)
        detail.sinceStart.shouldBeNull()
        detail.lowest.shouldBeNull()
        detail.freeToSpend.shouldBeNull()
        detail.comingUp shouldBe emptyList()
        detail.plannedDays shouldBe emptySet()
        detail.habits!!.purchases shouldBe 2
        detail.pace!!.previousMonth shouldBe Month.fromMonthValue(9)
        detail.outlook.shouldBeNull()
        detail.unusual.shouldBeNull()
    }

    @Test
    fun `a month ahead is all planned, and has no habits yet`() = runTest {
        // when
        val detail = useCase.detail(December, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.actualDays shouldBe 0
        detail.today.shouldBeNull()
        // All that's due from November's start has gone by then.
        detail.balances.first() shouldBe (10_449.70 plusOrMinus Cent)
        detail.habits.shouldBeNull()
        detail.pace.shouldBeNull()
        detail.freeToSpend.shouldBeNull()
    }

    @Test
    fun `the outlook spends ahead as the everyday days before did, on top of what's planned`() = runTest {
        // given 10 euros of everyday spending each day from October 2nd to yesterday, and a bill
        transactions.removeAll { it is Expense && it.settled && it.time.isBefore(at(9, 0)) }
        transactions += (2..31).map { day -> expense(day = day, month = 10, amount = 10.0, title = "Bakery") }
        transactions += (1..8).map { day -> expense(day = day, amount = 10.0, title = "Bakery") }
        transactions += expense(day = 15, month = 10, amount = 40.0, title = "Phone", recurring = true)

        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then every day ahead spends the 10 euros, and the bill was no everyday spending
        val outlook = detail.outlook!!
        outlook.perDay shouldBe (10.0 plusOrMinus Cent)
        outlook.balances.size shouldBe 22
        outlook.balances.first().likely shouldBe (detail.balances[8] plusOrMinus Cent)
        outlook.balances.forEachIndexed { step, band ->
            band.likely shouldBe (detail.balances[8 + step] - 10.0 * step plusOrMinus Cent)
            band.low shouldBe (band.likely plusOrMinus Cent)
            band.high shouldBe (band.likely plusOrMinus Cent)
        }
        // So far 80 of everyday spending and 20 on the card today; the fine overdue, the gym and the
        // card's bill to come.
        val spentSoFar = 80.0 + 20.0
        outlook.spending.first().likely shouldBe (spentSoFar + 15.0 plusOrMinus Cent)
        outlook.spending.last().likely shouldBe (spentSoFar + 15.0 + 35.0 + 550.0 + 210.0 plusOrMinus Cent)
        outlook.left!!.likely shouldBe (detail.freeToSpend!!.left - 210.0 plusOrMinus Cent)
        outlook.lowest!!.day shouldBe LocalDate.of(2026, 11, 30)
    }

    @Test
    fun `the outlook spreads as the days before did, and holds still all day`() = runTest {
        // when
        val outlook = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR").outlook!!

        // then
        outlook.balances.forEach { band ->
            band.low shouldBeLessThanOrEqualTo band.likely
            band.likely shouldBeLessThanOrEqualTo band.high
        }
        outlook.balances.last().high shouldBeLessThan outlook.balances.last().low + 1_000.0
        useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR").outlook shouldBe outlook
    }

    @Test
    fun `there's no outlook without four weeks since the first paid transaction`() = runTest {
        // given nothing paid before November
        transactions.removeAll { it.time.isBefore(at(1, 0)) }

        // when
        val detail = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR")

        // then
        detail.outlook.shouldBeNull()
    }

    @Test
    fun `what stands out is set against the months since the first paid transaction`() = runTest {
        // given, from May 1st to October, 50 euros of groceries by the 9th and small purchases
        transactions.removeAll { it.time.isBefore(at(1, 0)) }
        transactions += (5..10).flatMap { month ->
            listOf(
                expense(day = 1, month = month, amount = 50.0, title = "Lidl", category = Groceries),
                expense(day = 12, month = month, amount = 40.0, title = "Lidl", category = Groceries),
                expense(day = 4, month = month, amount = 30.0 + month, title = "Shoes", category = Shopping),
                expense(day = 20, month = month, amount = 20.0, title = "Socks", category = Shopping),
            )
        }
        // and, by November 9th, 120 euros of groceries in purchases of the usual size
        transactions += (5..8).map { day -> expense(day = day, amount = 30.0, title = "Lidl", category = Groceries) }

        // when
        val unusual = useCase.detail(November, startDayOfMonth = 1, baseCurrency = "EUR").unusual!!

        // then groceries took more than by the 9th of any of the 6 months; the 130.30 of home goods
        // are the biggest shopping in all of them, and the shopping this month, so it shows alone
        unusual.categories shouldBe listOf(
            UnusualCategory(Groceries, 120.0, usual = 50.0, beaten = 6, months = 6, higher = true)
        )
        unusual.purchases.map { it.title to it.since } shouldBe listOf("Home goods" to null)
        unusual.purchases.single().months shouldBe 6
    }

    private companion object {
        const val Cent = 0.01
        const val Evening = 2
        const val StartingBalance = 10_000.0
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

        val November = TimePeriod(month = Month.fromMonthValue(11), year = 2026)
        val October = TimePeriod(month = Month.fromMonthValue(10), year = 2026)
        val December = TimePeriod(month = Month.fromMonthValue(12), year = 2026)

        val Bank = account("Main Bank", n = 1)
        val Card = account("Credit card", n = 2, type = AccountType.CREDIT_CARD)
        val Hidden = account("Savings box", n = 3, inBalance = false)
        val Shopping = Category(
            id = CategoryId(UUID(9L, 1L)),
            name = NotBlankTrimmedString.unsafe("Shopping"),
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
        )
        val Groceries = Category(
            id = CategoryId(UUID(9L, 2L)),
            name = NotBlankTrimmedString.unsafe("Groceries"),
            color = ColorInt(0),
            icon = null,
            orderNum = 1.0,
        )

        /** November 9, 2026, noon, in UTC: a Monday. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 9, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        fun account(
            name: String,
            n: Long,
            currency: String = "EUR",
            type: AccountType = AccountType.BANK,
            inBalance: Boolean = true,
        ) = Account(
            id = AccountId(UUID(1L, n)),
            name = NotBlankTrimmedString.unsafe(name),
            asset = AssetCode.unsafe(currency),
            color = ColorInt(0),
            icon = null,
            includeInBalance = inBalance,
            type = type,
            archived = false,
            orderNum = n.toDouble(),
        )

        fun at(day: Int, hour: Int, month: Int = 11): Instant =
            LocalDateTime.of(2026, month, day, hour, 0).toInstant(ZoneOffset.UTC)

        fun expense(
            day: Int,
            amount: Double,
            title: String,
            month: Int = 11,
            hour: Int = 9,
            account: Account = Bank,
            category: Category? = null,
            bought: Instant? = null,
            loan: Boolean = false,
            paid: Boolean = true,
            recurring: Boolean = false,
        ) = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe(title),
            description = null,
            category = category?.id,
            time = at(day, hour, month),
            settled = paid,
            metadata = Metadata.copy(
                purchaseDateTime = bought,
                loanId = if (loan) UUID(7L, 7L) else null,
                recurringRuleId = if (recurring) UUID(8L, 8L) else null,
            ),
            tags = emptyList(),
            value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
            account = account.id,
        )

        fun income(day: Int, amount: Double, title: String, paid: Boolean = true) = Income(
            id = TransactionId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe(title),
            description = null,
            category = null,
            time = at(day, 9),
            settled = paid,
            metadata = Metadata,
            tags = emptyList(),
            value = PositiveValue(PositiveDouble.unsafe(amount), Eur),
            account = Bank.id,
        )

        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        fun Transaction.account(): AccountId = when (this) {
            is Income -> account
            is Expense -> account
            is Transfer -> fromAccount
        }

        // The fake wallet counts amounts as they are, all of its accounts being in euros.
        fun Transaction.signed(): Double = when (this) {
            is Income -> value.amount.value
            is Expense -> -value.amount.value
            is Transfer -> 0.0
        }
    }
}
