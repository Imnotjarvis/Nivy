package com.nivy.transactions

import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.transaction.TransactionAmountsUseCase
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.utils.computationThread
import com.nivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class CategoryPeriodLoaderTest {

    private val bank = account("Bank")
    private val cash = account("Cash")
    private val food = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Food"),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )

    // It's November 15: due on the 20th is upcoming, on the 10th overdue.
    private val bankUpcoming = expense(bank, 40.0, "2026-11-20T10:00:00Z")
    private val cashUpcoming = expense(cash, 15.0, "2026-11-20T10:00:00Z")
    private val bankOverdue = expense(bank, 60.0, "2026-11-10T10:00:00Z")
    private val cashOverdue = expense(cash, 5.0, "2026-11-10T10:00:00Z")

    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAllDueToBetweenByCategory(any(), any(), any()) } returns
            listOf(bankUpcoming, cashUpcoming, bankOverdue, cashOverdue)
        coEvery { findAllByCategoryAndTypeAndBetween(any(), any(), any(), any()) } returns emptyList()
        coEvery { findAllByCategoryAndBetween(any(), any(), any()) } returns emptyList()
    }
    private val amounts = mockk<TransactionAmountsUseCase> {
        coEvery { total(any(), any()) } answers {
            firstArg<List<Transaction>>().sumOf { (it as Expense).value.amount.value }
        }
        coEvery { balance(any(), any()) } returns 0.0
    }
    private val currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur }

    private val loader = CategoryPeriodLoader(
        periodState = PeriodState(FixedTime),
        categoryLogic = CategoryTransactionsLogic(
            transactionRepository = transactionRepository,
            currencyRepository = currencyRepository,
            transactionAmountsUseCase = amounts,
            transactionsWithDateDividersUseCase = mockk(relaxed = true),
            timeProvider = FixedTime,
        ),
        transactionsWithDateDividersUseCase = mockk(relaxed = true),
        transactionsIncomeExpenseUseCase = mockk(relaxed = true),
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        resourceProvider = mockk(relaxed = true),
    )

    @Before
    fun setUp() {
        mockkStatic("com.nivy.wallet.utils.MVVMExtKt")
        coEvery { ioThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { computationThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `a category's page for one account lists and totals only that account's due payments`() = runTest {
        // given
        val state = newState()

        // when
        loader.initForCategory(state, food, accountFilterList = listOf(bank.id.value))

        // then
        state.upcoming.value shouldBe listOf(bankUpcoming)
        state.upcomingExpenses.doubleValue shouldBe 40.0
        state.overdue.value shouldBe listOf(bankOverdue)
        state.overdueExpenses.doubleValue shouldBe 60.0
    }

    @Test
    fun `a category's page for every account lists all its due payments`() = runTest {
        // given
        val state = newState()

        // when
        loader.initForCategory(state, food, accountFilterList = emptyList())

        // then
        state.upcoming.value shouldBe listOf(bankUpcoming, cashUpcoming)
        state.upcomingExpenses.doubleValue shouldBe 55.0
        state.overdue.value shouldBe listOf(bankOverdue, cashOverdue)
        state.overdueExpenses.doubleValue shouldBe 65.0
    }

    private fun newState() = CategoryLoadState(
        period = mutableStateOf(TimePeriod(month = Month.fromMonthValue(11), year = 2026)),
        baseCurrency = mutableStateOf("EUR"),
        accounts = mutableStateOf(persistentListOf(bank, cash)),
        category = mutableStateOf(null),
        initWithTransactions = mutableStateOf(false),
        balance = mutableDoubleStateOf(0.0),
        income = mutableDoubleStateOf(0.0),
        expenses = mutableDoubleStateOf(0.0),
        history = mutableStateOf(persistentListOf()),
        upcoming = mutableStateOf(persistentListOf()),
        upcomingIncome = mutableDoubleStateOf(0.0),
        upcomingExpenses = mutableDoubleStateOf(0.0),
        overdue = mutableStateOf(persistentListOf()),
        overdueIncome = mutableDoubleStateOf(0.0),
        overdueExpenses = mutableDoubleStateOf(0.0),
    )

    private fun expense(account: Account, amount: Double, due: String) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = food.id,
        time = Instant.parse(due),
        settled = false,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), Eur),
        account = account.id,
    )

    private fun account(name: String) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = Eur,
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")

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
