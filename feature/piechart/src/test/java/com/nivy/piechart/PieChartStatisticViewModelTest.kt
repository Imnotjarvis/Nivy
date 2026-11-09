package com.nivy.piechart

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
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
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.TransactionRepository
import com.nivy.navigation.BudgetPage
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.piechart.usecase.PieChartData
import com.nivy.piechart.usecase.PieChartUseCase
import com.nivy.ui.testing.runComposeTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class PieChartStatisticViewModelTest {

    private val bank = AccountId(UUID.randomUUID())
    private val groceries = CategoryId(UUID.randomUUID())
    private val rent = CategoryId(UUID.randomUUID())

    private val lunch = expense(25.0, "2026-11-03T12:00:00Z", groceries)
    private val dinner = expense(40.0, "2026-11-05T20:00:00Z", groceries)
    private var stored = listOf<Transaction>(lunch, dinner)

    private val november = ClosedTimeRange(
        from = Instant.parse("2026-11-01T00:00:00Z"),
        to = Instant.parse("2026-11-30T23:59:59Z"),
    )
    private val food = PieChartStatisticScreen(
        type = TransactionType.EXPENSE,
        budgetPage = BudgetPage(budget = budget("Food", listOf(groceries)), month = "November 2026", range = november),
    )

    private val pieChartUseCase = mockk<PieChartUseCase> {
        coEvery { pieChart(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            PieChartData(totalAmount = 0.0, categoryAmounts = persistentListOf())
    }
    private val transactionRepository = mockk<TransactionRepository> {
        coEvery { findAllBetween(any(), any()) } answers { stored }
    }
    private val appPreferences = mockk<AppPreferences> {
        coEvery { transfersAsIncomeExpense() } returns false
        coEvery { pieChartByAccount() } returns false
        coEvery { setPieChartByAccount(any()) } just Runs
    }
    private val viewModel = PieChartStatisticViewModel(
        currencyRepository = mockk { coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR") },
        periodState = PeriodState(FixedTime),
        pieChartUseCase = pieChartUseCase,
        appPreferences = appPreferences,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        accountRepository = mockk { coEvery { findAll() } returns listOf(account(bank)) },
        categoryRepository = mockk { coEvery { findAll() } returns emptyList() },
        transactionsWithDateDividersUseCase = mockk { coEvery { withDateDividers(any(), any()) } returns emptyList() },
        features = mockk(relaxed = true),
        transactionRepository = transactionRepository,
        dispatchers = TestDispatchersProvider,
    )

    @Before
    fun setUp() {
        mockkStatic("com.nivy.wallet.utils.MVVMExtKt")
        coEvery { ioThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `a budget's page shows its expenses as they are now, without one deleted since`() {
        viewModel.runTest(events = listOf(PieChartStatisticEvent.OnStart(food))) {
            transactions shouldBe listOf(lunch, dinner)
        }

        // The dinner is deleted from the page; back on it, the page shows again.
        stored = listOf(lunch)

        viewModel.runTest(events = listOf(PieChartStatisticEvent.OnStart(food))) {
            transactions shouldBe listOf(lunch)
        }
    }

    @Test
    fun `a budget's page lists only the budget's expenses, of its month`() {
        stored = listOf(lunch, expense(800.0, "2026-11-01T09:00:00Z", rent))

        viewModel.runTest(events = listOf(PieChartStatisticEvent.OnStart(food))) {
            transactions shouldBe listOf(lunch)
        }
        coVerify { transactionRepository.findAllBetween(november.from, november.to) }
    }

    @Test
    fun `a month chosen after another stays chosen, even when the other one's load ends last`() {
        // December's chart comes slowly: January is chosen, and comes, before it does.
        val december = CompletableDeferred<Unit>()
        coEvery { pieChartUseCase.pieChart(any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            val month = secondArg<FromToTimeRange>().from!!.atZone(ZoneId.of("UTC")).monthValue
            if (month == 12) december.await()
            PieChartData(totalAmount = month.toDouble(), categoryAmounts = persistentListOf())
        }

        val expenses = PieChartStatisticScreen(type = TransactionType.EXPENSE)
        val januaryPeriod = TimePeriod(month = Month.fromMonthValue(1), year = 2027)
        runComposeTest(
            present = { viewModel.uiState() },
            actions = {
                viewModel.onEvent(PieChartStatisticEvent.OnStart(expenses))
                viewModel.onEvent(
                    PieChartStatisticEvent.OnSetPeriod(TimePeriod(month = Month.fromMonthValue(12), year = 2026))
                )
                viewModel.onEvent(PieChartStatisticEvent.OnSetPeriod(januaryPeriod))
                december.complete(Unit)
            },
        ) {
            period shouldBe januaryPeriod
            totalAmount shouldBe 1.0
        }
    }

    @Test
    fun `split by account where the chart covers more than one, and the split is remembered`() {
        coEvery { pieChartUseCase.pieChart(any(), any(), any(), any(), any(), any(), any(), any()) } returns byAccount

        viewModel.runTest(events = listOf(start, PieChartStatisticEvent.OnBreakdownSelected(Breakdown.Accounts))) {
            showBreakdownToggle shouldBe true
            breakdown shouldBe Breakdown.Accounts
            accountAmounts shouldBe byAccount.accountAmounts
        }
        coVerify { appPreferences.setPieChartByAccount(true) }
    }

    @Test
    fun `the chart opens split as last chosen`() {
        coEvery { appPreferences.pieChartByAccount() } returns true
        coEvery { pieChartUseCase.pieChart(any(), any(), any(), any(), any(), any(), any(), any()) } returns byAccount

        viewModel.runTest(events = listOf(start)) {
            breakdown shouldBe Breakdown.Accounts
        }
    }

    @Test
    fun `with one account, the chart splits by category, whatever was chosen`() {
        coEvery { appPreferences.pieChartByAccount() } returns true
        coEvery { pieChartUseCase.pieChart(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            byAccount.copy(accountsCovered = 1)

        viewModel.runTest(events = listOf(start)) {
            showBreakdownToggle shouldBe false
            breakdown shouldBe Breakdown.Categories
        }
    }

    @Test
    fun `a tapped account is selected, its card first, and a second tap clears it`() {
        coEvery { pieChartUseCase.pieChart(any(), any(), any(), any(), any(), any(), any(), any()) } returns byAccount
        val selectCard = PieChartStatisticEvent.OnAccountClicked(card)

        viewModel.runTest(events = listOf(start, selectCard)) {
            selectedAccount shouldBe card
            accountAmounts.map { it.account } shouldBe listOf(card, account(bank))
        }
        viewModel.runTest(events = listOf(selectCard)) {
            selectedAccount shouldBe null
            accountAmounts.map { it.account } shouldBe listOf(account(bank), card)
        }
    }

    private val start = PieChartStatisticEvent.OnStart(PieChartStatisticScreen(type = TransactionType.EXPENSE))
    private val card = account(AccountId(UUID.randomUUID())).copy(name = NotBlankTrimmedString.unsafe("Card"))

    // The bank paid 70.00 EUR of the month's expenses, the card 20.00 EUR.
    private val byAccount
        get() = PieChartData(
            totalAmount = 90.0,
            categoryAmounts = persistentListOf(),
            accountAmounts = persistentListOf(AccountAmount(account(bank), 70.0), AccountAmount(card, 20.0)),
            accountsCovered = 2,
        )

    private fun budget(name: String, categories: List<CategoryId>) = Budget(
        id = BudgetId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        amount = PositiveDouble.unsafe(400.0),
        categories = categories,
        accounts = emptyList(),
        orderNum = 0.0,
    )

    private fun expense(amount: Double, time: String, category: CategoryId) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = category,
        time = Instant.parse(time),
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        ),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe("EUR")),
        account = bank,
    )

    private fun account(id: AccountId) = Account(
        id = id,
        name = NotBlankTrimmedString.unsafe("Bank"),
        asset = AssetCode.unsafe("EUR"),
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )

    private companion object {
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
