package com.nivy.transactions

import arrow.core.Some
import com.nivy.base.TestDispatchersProvider
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
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.account.AccountBalanceDetails
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.account.AccountIncomeExpense
import com.nivy.domain.usecase.account.AccountIncomeExpenseUseCase
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.navigation.Navigation
import com.nivy.navigation.TransactionsScreen
import com.nivy.ui.testing.runComposeTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val bank = account("Bank", Eur)
    private val dollars = account("Dollars", Usd)
    private val bankPage = TransactionsScreen(accountId = bank.id.value)

    /** The income of the month a range starts in: November 50, December 70, any other 0. */
    private fun incomeOf(range: ClosedTimeRange): Int = when (range.from) {
        Instant.parse("2026-11-01T00:00:00Z") -> 50
        Instant.parse("2026-12-01T00:00:00Z") -> 70
        else -> 0
    }

    private val accountRepository = mockk<AccountRepository> {
        coEvery { findById(bank.id) } returns bank
        coEvery { findById(dollars.id) } returns dollars
        coEvery { findAll() } returns listOf(bank, dollars)
    }
    private val accountIncomeExpenseUseCase = mockk<AccountIncomeExpenseUseCase> {
        coEvery { incomeExpense(any(), any(), any()) } answers {
            AccountIncomeExpense(
                account = firstArg(),
                incomeExpensePair = IncomeExpensePair(
                    income = incomeOf(secondArg()).toBigDecimal(),
                    expense = BigDecimal(30),
                ),
            )
        }
    }
    private var accountTransactions = listOf<Transaction>()
    private val shownInHistory = slot<List<Transaction>>()
    private val transactionsWithDateDividersUseCase = mockk<TransactionsWithDateDividersUseCase> {
        coEvery { withDateDividers(any(), capture(shownInHistory)) } returns emptyList()
    }
    private val accountCategoryDeletion = mockk<AccountCategoryDeletion>(relaxUnitFun = true)
    private val categoryPeriodLoader = mockk<CategoryPeriodLoader>(relaxUnitFun = true)
    private val dueTransactionsUseCase = mockk<DueTransactionsUseCase>(relaxUnitFun = true) {
        coEvery { pay(any()) } returns null
    }
    private val accountBalanceUseCase = mockk<AccountBalanceUseCase> {
        coEvery { balance(any(), any()) } answers { AccountBalanceDetails(firstArg(), BigDecimal(120)) }
    }
    private val periodState = PeriodState(FixedTime)
    private val categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() }
    private val nav = mockk<Navigation>(relaxed = true)

    private val viewModel = TransactionsViewModel(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        periodState = periodState,
        accountLogic = mockk<AccountTransactionsLogic> {
            coEvery { calculateUpcomingIncome(any(), any()) } returns 0.0
            coEvery { calculateUpcomingExpenses(any(), any()) } returns 0.0
            coEvery { calculateOverdueIncome(any(), any()) } returns 0.0
            coEvery { calculateOverdueExpenses(any(), any()) } returns 0.0
            coEvery { upcoming(any(), any()) } returns emptyList()
            coEvery { overdue(any(), any()) } returns emptyList()
        },
        categoryPeriodLoader = categoryPeriodLoader,
        accountInvestmentData = mockk(relaxed = true),
        accountCategoryDeletion = accountCategoryDeletion,
        categoryCreator = mockk(relaxed = true),
        accountCreator = mockk(relaxed = true),
        dueTransactionsUseCase = dueTransactionsUseCase,
        appPreferences = mockk<AppPreferences> { coEvery { transfersAsIncomeExpense() } returns false },
        accountTransactionsUseCase = mockk<AccountTransactionsUseCase> {
            coEvery { transactions(any(), any()) } answers { accountTransactions }
        },
        transactionsWithDateDividersUseCase = transactionsWithDateDividersUseCase,
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        accountBalanceUseCase = accountBalanceUseCase,
        accountIncomeExpenseUseCase = accountIncomeExpenseUseCase,
        exchangeUseCase = mockk<ExchangeUseCase> {
            // 1 USD = 0.5 EUR
            coEvery { convert(any(), any()) } answers {
                Some(PositiveValue(PositiveDouble.unsafe(firstArg<PositiveValue>().amount.value / 2), Eur))
            }
        },
        creditCardRepository = mockk<CreditCardRepository> {
            coEvery { findByAccount(any()) } returns emptyList()
        },
        accountValueRepository = mockk(relaxUnitFun = true),
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        features = mockk(relaxed = true),
        userWrites = UserWrites(mockk(relaxed = true)),
        dispatchers = TestDispatchersProvider,
        nav = nav,
    )

    @Before
    fun setUp() {
        // start() runs in viewModelScope before runTest() sets its own main dispatcher.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an account's page shows its balance and the month's income and expenses`() {
        // when
        viewModel.start(bankPage)

        // then
        viewModel.runTest {
            account shouldBe bank
            balance shouldBe 120.0
            income shouldBe 50.0
            expenses shouldBe 30.0
        }
    }

    @Test
    fun `an account in another currency also shows its balance in the base currency`() {
        // when
        viewModel.start(TransactionsScreen(accountId = dollars.id.value))

        // then
        viewModel.runTest {
            currency shouldBe "USD"
            balance shouldBe 120.0
            balanceBaseCurrency shouldBe 60.0
        }
    }

    @Test
    fun `an account in the base currency has no converted balance`() {
        // when
        viewModel.start(bankPage)

        // then
        viewModel.runTest {
            balanceBaseCurrency shouldBe null
        }
    }

    @Test
    fun `the history leaves out what isn't paid yet`() {
        // given
        val paid = expense(bank, settled = true)
        val due = expense(bank, settled = false)
        accountTransactions = listOf(paid, due)

        // when
        viewModel.start(bankPage)

        // then
        shownInHistory.captured shouldBe listOf(paid)
    }

    @Test
    fun `the next month's arrow shows the next month and its figures`() {
        // given
        viewModel.start(bankPage)

        // when
        viewModel.runTest(events = listOf(TransactionsEvent.NextMonth(bankPage))) {
            // then
            period shouldBe TimePeriod(month = December, year = 2026)
            income shouldBe 70.0
        }
    }

    @Test
    fun `a month chosen after another shows its own figures, even when the other one's load ends last`() {
        // December's figures come slowly: January is chosen, and comes, before they do.
        val december = CompletableDeferred<Unit>()
        coEvery { accountIncomeExpenseUseCase.incomeExpense(any(), any(), any()) } coAnswers {
            val range = secondArg<ClosedTimeRange>()
            if (range.from == Instant.parse("2026-12-01T00:00:00Z")) december.await()
            AccountIncomeExpense(
                account = firstArg(),
                incomeExpensePair = IncomeExpensePair(
                    income = incomeOf(range).toBigDecimal(),
                    expense = BigDecimal(30),
                ),
            )
        }
        viewModel.start(bankPage)

        runComposeTest(
            present = { viewModel.uiState() },
            actions = {
                viewModel.onEvent(TransactionsEvent.NextMonth(bankPage))
                viewModel.onEvent(TransactionsEvent.NextMonth(bankPage))
                december.complete(Unit)
            },
        ) {
            period shouldBe TimePeriod(month = January, year = 2027)
            income shouldBe 0.0
        }
    }

    @Test
    fun `the previous month's arrow goes back across a year`() {
        // given
        periodState.select(TimePeriod(month = January, year = 2027))
        viewModel.start(bankPage)

        // when
        viewModel.runTest(events = listOf(TransactionsEvent.PreviousMonth(bankPage))) {
            // then
            period shouldBe TimePeriod(month = December, year = 2026)
            income shouldBe 70.0
        }
    }

    @Test
    fun `the arrows do nothing when the period isn't a month`() {
        // given
        val range = TimePeriod(
            fromToRange = FromToTimeRange(
                from = Instant.parse("2026-11-01T00:00:00Z"),
                to = Instant.parse("2026-11-10T00:00:00Z"),
            )
        )
        viewModel.start(bankPage, timePeriod = range)

        // when
        viewModel.runTest(events = listOf(TransactionsEvent.NextMonth(bankPage))) {
            // then
            period shouldBe range
        }
    }

    @Test
    fun `a page whose account is gone goes back instead of loading`() {
        // given the account deleted on a page opened from this one, which went back here
        coEvery { accountRepository.findById(bank.id) } returns null

        // when
        viewModel.start(bankPage)

        // then
        viewModel.runTest { account shouldBe null }
        verify { nav.back() }
    }

    @Test
    fun `a page whose category is gone goes back instead of loading`() {
        // given
        val gone = CategoryId(UUID.randomUUID())
        coEvery { categoryRepository.findById(gone) } returns null

        // when
        viewModel.start(TransactionsScreen(categoryId = gone.value))

        // then
        viewModel.runTest { category shouldBe null }
        verify { nav.back() }
    }

    @Test
    fun `deleting needs the page's exact name`() {
        // given
        viewModel.start(bankPage)

        viewModel.runTest(events = listOf(TransactionsEvent.UpdateAccountDeletionState("Ban"))) {
            enableDeletionButton shouldBe false
        }
        viewModel.runTest(events = listOf(TransactionsEvent.UpdateAccountDeletionState("Bank"))) {
            enableDeletionButton shouldBe true
        }
    }

    @Test
    fun `deleting an account's page deletes the account, a category's the category`() {
        // given
        val categoryId = UUID.randomUUID()
        val categoryPage = TransactionsScreen(categoryId = categoryId)

        // when
        viewModel.onEvent(TransactionsEvent.Delete(bankPage))
        viewModel.onEvent(TransactionsEvent.Delete(categoryPage))

        // then
        coVerify(exactly = 1) { accountCategoryDeletion.deleteAccount(bank.id.value) }
        coVerify(exactly = 1) { accountCategoryDeletion.deleteCategory(categoryId) }
    }

    @Test
    fun `paying a due transaction pays it, then refreshes the page`() {
        // given
        val due = expense(bank, settled = false)
        viewModel.start(bankPage)

        // when
        viewModel.onEvent(TransactionsEvent.PayOrGet(bankPage, due))

        // then
        coVerify(exactly = 1) { dueTransactionsUseCase.pay(due) }
        coVerify(exactly = 2) { accountBalanceUseCase.balance(bank, any()) }
    }

    @Test
    fun `a category's page loads its own figures, or those Reports passed`() {
        // given
        val food = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Food"),
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
        )
        coEvery { categoryRepository.findById(food.id) } returns food
        val fromReports = listOf<Transaction>(expense(bank, settled = true))

        // when
        viewModel.start(TransactionsScreen(categoryId = food.id.value))
        viewModel.start(TransactionsScreen(categoryId = food.id.value, transactions = fromReports))

        // then
        coVerify(exactly = 1) { categoryPeriodLoader.initForCategory(any(), food, any()) }
        coVerify(exactly = 1) {
            categoryPeriodLoader.initForCategoryWithTransactions(any(), food, any(), fromReports)
        }
    }

    private fun expense(account: Account, settled: Boolean) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse("2026-11-10T10:00:00Z"),
        settled = settled,
        metadata = Metadata,
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(10.0), account.asset),
        account = account.id,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")
        val December = Month.fromMonthValue(12)
        val January = Month.fromMonthValue(1)

        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
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

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
