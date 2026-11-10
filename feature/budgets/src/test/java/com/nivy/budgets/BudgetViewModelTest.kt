package com.nivy.budgets

import arrow.core.Some
import com.nivy.base.model.AccountType
import com.nivy.base.model.Theme
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
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
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.BudgetRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.navigation.BudgetScreen
import com.nivy.ui.testing.ComposeViewModelTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.Month
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class BudgetViewModelTest : ComposeViewModelTest() {

    private val bank = account("Bank")
    private val card = account("Card")
    private val dollars = account("Dollars", Usd)
    private val offBalance = account("Savings box", includeInBalance = false)

    private val groceries = CategoryId(UUID.randomUUID())
    private val rent = CategoryId(UUID.randomUUID())

    private val budgetRepository = mockk<BudgetRepository>(relaxUnitFun = true)
    private val appPreferences = mockk<AppPreferences>(relaxUnitFun = true)
    private var budgets = listOf<Budget>()
    private var transactions = listOf<Transaction>()
    private var savingsGoal = 0.0

    private val opening = BudgetScreen()
    private lateinit var viewModel: BudgetViewModel

    @Before
    fun setup() {
        coEvery { budgetRepository.findAll() } answers { budgets }
        coEvery { budgetRepository.findMaxOrderNum() } answers { budgets.maxOfOrNull { it.orderNum } ?: 0.0 }
        val transactionRepository = mockk<TransactionRepository> {
            coEvery { findAllBetween(any(), any()) } answers {
                val (from, to) = firstArg<Instant>() to secondArg<Instant>()
                transactions.filter { it.time in from..to }
            }
        }
        coEvery { appPreferences.startDayOfMonth() } returns 1
        coEvery { appPreferences.settings() } answers {
            AppSettings(theme = Theme.AUTO, baseCurrency = "EUR", bufferAmount = savingsGoal, name = "")
        }
        viewModel = BudgetViewModel(
            appPreferences = appPreferences,
            budgetRepository = budgetRepository,
            periodState = mockk<PeriodState>(relaxed = true),
            categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
            accountRepository = mockk<AccountRepository> {
                coEvery { findAll() } returns listOf(bank, card, dollars, offBalance)
            },
            currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
            transactionRepository = transactionRepository,
            exchangeUseCase = mockk<ExchangeUseCase> {
                // 1 USD = 0.5 EUR
                coEvery { convert(any<PositiveValue>(), Eur) } answers {
                    val value = firstArg<PositiveValue>()
                    val rate = if (value.asset == Usd) 0.5 else 1.0
                    Some(PositiveValue(PositiveDouble.unsafe(value.amount.value * rate), Eur))
                }
            },
            timeProvider = FixedTime,
            timeConverter = TestTimeConverter(),
            userWrites = UserWrites(mockk(relaxed = true)),
        )
    }

    @Test
    fun `a budget for some accounts counts only their expenses`() {
        budgets = listOf(budget("Card", accounts = listOf(card.id)), budget("All"))
        transactions = listOf(
            expense(card, 30.0, "2026-11-03T10:00:00Z"),
            expense(bank, 20.0, "2026-11-04T10:00:00Z"),
        )

        openBudgets {
            spentOf("Card") shouldBe 30.0
            spentOf("All") shouldBe 50.0
        }
    }

    @Test
    fun `a category budget counts only its categories' expenses`() {
        budgets = listOf(budget("Groceries", categories = listOf(groceries)))
        transactions = listOf(
            expense(bank, 30.0, "2026-11-03T10:00:00Z", groceries),
            expense(bank, 500.0, "2026-11-01T10:00:00Z", rent),
            expense(bank, 5.0, "2026-11-05T10:00:00Z", category = null),
        )

        openBudgets {
            spentOf("Groceries") shouldBe 30.0
        }
    }

    @Test
    fun `only expenses count, not income or transfers`() {
        budgets = listOf(budget("All"))
        transactions = listOf(
            expense(bank, 30.0, "2026-11-03T10:00:00Z"),
            income(bank, 1000.0, "2026-11-01T10:00:00Z"),
            transfer(bank, card, 200.0, "2026-11-02T10:00:00Z"),
        )

        openBudgets {
            spentOf("All") shouldBe 30.0
        }
    }

    @Test
    fun `spending in another currency counts in the base currency`() {
        budgets = listOf(budget("Dollars", accounts = listOf(dollars.id)))
        transactions = listOf(expense(dollars, 10.0, "2026-11-03T10:00:00Z"))

        openBudgets {
            spentOf("Dollars") shouldBe 5.0
            baseCurrency shouldBe "EUR"
        }
    }

    @Test
    fun `the month saved its income less its expenses, transfers aside`() {
        savingsGoal = 500.0
        transactions = listOf(
            income(bank, 1000.0, "2026-11-01T10:00:00Z"),
            expense(card, 300.0, "2026-11-03T10:00:00Z"),
            expense(dollars, 100.0, "2026-11-04T10:00:00Z"),
            transfer(bank, card, 200.0, "2026-11-02T10:00:00Z"),
        )

        openBudgets {
            savings shouldBe SavingsGoal(goal = 500.0, saved = 650.0)
        }
    }

    @Test
    fun `savings leave out accounts outside the balance and unpaid transactions`() {
        transactions = listOf(
            income(offBalance, 1000.0, "2026-11-01T10:00:00Z"),
            expense(bank, 40.0, "2026-11-03T10:00:00Z"),
            expense(bank, 60.0, "2026-11-05T10:00:00Z", settled = false),
        )

        openBudgets {
            savings.saved shouldBe -40.0
        }
    }

    @Test
    fun `setting the savings goal stores it`() {
        openBudgets(BudgetScreenEvent.OnSetSavingsGoal(300.0))

        coVerify { appPreferences.setBufferAmount(300.0) }
    }

    @Test
    fun `the previous month shows what was spent then`() {
        budgets = listOf(budget("All"))
        transactions = listOf(
            expense(bank, 40.0, "2026-10-20T10:00:00Z"),
            expense(bank, 30.0, "2026-11-03T10:00:00Z"),
        )

        openBudgets(BudgetScreenEvent.OnPreviousMonth) {
            period?.month shouldBe Month.fromMonthValue(10)
            spentOf("All") shouldBe 40.0
        }
    }

    @Test
    fun `back on Budgets, from a budget's page, it shows the month it showed`() {
        openBudgets(BudgetScreenEvent.OnPreviousMonth, BudgetScreenEvent.OnOpen(opening)) {
            period?.month shouldBe Month.fromMonthValue(10)
        }
    }

    @Test
    fun `Budgets opened anew shows the current month`() {
        openBudgets(BudgetScreenEvent.OnPreviousMonth, BudgetScreenEvent.OnOpen(BudgetScreen())) {
            period?.month shouldBe Month.fromMonthValue(11)
        }
    }

    @Test
    fun `a new budget keeps its accounts and goes last`() {
        budgets = listOf(budget("First", orderNum = 0.0), budget("Second", orderNum = 1.0))

        openBudgets(
            BudgetScreenEvent.OnCreateBudget(
                name = NotBlankTrimmedString.unsafe("Card"),
                amount = PositiveDouble.unsafe(300.0),
                categories = listOf(groceries),
                accounts = listOf(card.id),
            )
        )

        coVerify {
            budgetRepository.save(
                match {
                    it.name.value == "Card" && it.amount.value == 300.0 &&
                        it.categories == listOf(groceries) && it.accounts == listOf(card.id) &&
                        it.orderNum == 2.0
                }
            )
        }
    }

    // Budgets loads as it's opened, here from Home.
    private fun openBudgets(vararg events: BudgetScreenEvent, verify: BudgetScreenState.() -> Unit = {}) =
        viewModel.runTest(
            events = listOf(BudgetScreenEvent.OnOpen(opening)) + events,
            context = paparazzi.context,
            verify = verify,
        )

    private fun BudgetScreenState.spentOf(name: String): Double =
        budgets.single { it.budget.name.value == name }.spentAmount

    private fun budget(
        name: String,
        amount: Double = 500.0,
        categories: List<CategoryId> = emptyList(),
        accounts: List<AccountId> = emptyList(),
        orderNum: Double = 0.0,
    ) = Budget(
        id = BudgetId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        amount = PositiveDouble.unsafe(amount),
        categories = categories,
        accounts = accounts,
        orderNum = orderNum,
    )

    private fun expense(
        account: Account,
        amount: Double,
        time: String,
        category: CategoryId? = null,
        settled: Boolean = true,
    ) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = category,
        time = Instant.parse(time),
        settled = settled,
        metadata = Metadata,
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
        account = account.id,
    )

    private fun income(account: Account, amount: Double, time: String) = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
        account = account.id,
    )

    private fun transfer(from: Account, to: Account, amount: Double, time: String) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        fromAccount = from.id,
        fromValue = PositiveValue(PositiveDouble.unsafe(amount), from.asset),
        toAccount = to.id,
        toValue = PositiveValue(PositiveDouble.unsafe(amount), to.asset),
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

        /** November 15, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }

        fun account(name: String, asset: AssetCode = Eur, includeInBalance: Boolean = true) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = includeInBalance,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
