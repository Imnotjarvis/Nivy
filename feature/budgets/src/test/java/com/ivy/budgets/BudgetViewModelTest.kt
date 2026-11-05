package com.ivy.budgets

import arrow.core.Some
import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Budget
import com.ivy.data.model.BudgetId
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.BudgetRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import com.ivy.wallet.PeriodState
import com.ivy.wallet.data.model.Month
import com.ivy.wallet.utils.format
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

    private val groceries = CategoryId(UUID.randomUUID())
    private val rent = CategoryId(UUID.randomUUID())

    private val budgetRepository = mockk<BudgetRepository>(relaxUnitFun = true)
    private var budgets = listOf<Budget>()
    private var transactions = listOf<Transaction>()

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
        viewModel = BudgetViewModel(
            appPreferences = mockk<AppPreferences> { coEvery { startDayOfMonth() } returns 1 },
            budgetRepository = budgetRepository,
            periodState = mockk<PeriodState>(relaxed = true),
            categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns emptyList() },
            accountRepository = mockk<AccountRepository> {
                coEvery { findAll() } returns listOf(bank, card, dollars)
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
        )
    }

    @Test
    fun `a budget for some accounts counts only their expenses`() {
        budgets = listOf(budget("Card", accounts = listOf(card.id)), budget("All"))
        transactions = listOf(
            expense(card, 30.0, "2026-11-03T10:00:00Z"),
            expense(bank, 20.0, "2026-11-04T10:00:00Z"),
        )

        viewModel.runTest(context = paparazzi.context) {
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

        viewModel.runTest(context = paparazzi.context) {
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

        viewModel.runTest(context = paparazzi.context) {
            spentOf("All") shouldBe 30.0
        }
    }

    @Test
    fun `spending in another currency counts in the base currency`() {
        budgets = listOf(budget("Dollars", accounts = listOf(dollars.id)))
        transactions = listOf(expense(dollars, 10.0, "2026-11-03T10:00:00Z"))

        viewModel.runTest(context = paparazzi.context) {
            spentOf("Dollars") shouldBe 5.0
            baseCurrency shouldBe "EUR"
        }
    }

    @Test
    fun `remaining budget is what category budgets have left`() {
        budgets = listOf(
            budget("Groceries", amount = 100.0, categories = listOf(groceries)),
            budget("Total", amount = 1000.0),
        )
        transactions = listOf(expense(bank, 30.0, "2026-11-03T10:00:00Z", groceries))

        viewModel.runTest(context = paparazzi.context) {
            categoryBudgetsTotal shouldBe 100.0
            appBudgetMax shouldBe 1000.0
            totalRemainingBudgetText shouldBe "Total Remaining Budget: ${70.0.format("EUR")} EUR"
        }
    }

    @Test
    fun `an exceeded category budget says by how much`() {
        budgets = listOf(budget("Groceries", amount = 100.0, categories = listOf(groceries)))
        transactions = listOf(expense(bank, 130.0, "2026-11-03T10:00:00Z", groceries))

        viewModel.runTest(context = paparazzi.context) {
            totalRemainingBudgetText shouldBe "Budget exceeded by ${30.0.format("EUR")} EUR"
        }
    }

    @Test
    fun `the previous month shows what was spent then`() {
        budgets = listOf(budget("All"))
        transactions = listOf(
            expense(bank, 40.0, "2026-10-20T10:00:00Z"),
            expense(bank, 30.0, "2026-11-03T10:00:00Z"),
        )

        viewModel.runTest(
            events = listOf(BudgetScreenEvent.OnPreviousMonth),
            context = paparazzi.context,
        ) {
            period?.month shouldBe Month.fromMonthValue(10)
            spentOf("All") shouldBe 40.0
        }
    }

    @Test
    fun `a new budget keeps its accounts and goes last`() {
        budgets = listOf(budget("First", orderNum = 0.0), budget("Second", orderNum = 1.0))

        viewModel.runTest(
            events = listOf(
                BudgetScreenEvent.OnCreateBudget(
                    name = NotBlankTrimmedString.unsafe("Card"),
                    amount = PositiveDouble.unsafe(300.0),
                    categories = listOf(groceries),
                    accounts = listOf(card.id),
                )
            ),
            context = paparazzi.context,
        ) {}

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
    ) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = category,
        time = Instant.parse(time),
        settled = true,
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

        fun account(name: String, asset: AssetCode = Eur) = Account(
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
