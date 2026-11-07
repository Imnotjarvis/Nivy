package com.nivy.reports

import android.content.Context
import android.net.Uri
import arrow.core.Some
import arrow.core.left
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.Toaster
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.base.resource.ResourceProvider
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.file.FileSystem
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TagAssociation
import com.nivy.data.model.TagId
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TagRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.RootScreen
import com.nivy.domain.usecase.csv.ExportCsvUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.ui.R
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class ReportViewModelTest {

    private val bank = account("Bank", Eur)
    private val dollars = account("Dollars", Usd)
    private val food = category("Food")
    private val rent = category("Rent")

    private val groceries = expense(bank, 20.0, "2026-11-03T10:00:00Z", food, title = "Groceries")
    private val flatRent = expense(bank, 100.0, "2026-11-01T10:00:00Z", rent, description = "November RENT")
    private val salary = income(bank, 1_000.0, "2026-11-05T10:00:00Z")
    private val toDollars = transfer(bank, dollars, 50.0, "2026-11-06T10:00:00Z")
    private val diner = expense(dollars, 30.0, "2026-11-07T10:00:00Z", food, title = "Diner")
    private val october = expense(bank, 5.0, "2026-10-20T10:00:00Z", food)

    // Unpaid, either side of today, November 15
    private val upcoming = expense(bank, 40.0, "2026-11-25T10:00:00Z", food, settled = false)
    private val overdue = expense(bank, 60.0, "2026-11-10T10:00:00Z", food, settled = false)

    private var transactions =
        listOf<Transaction>(groceries, flatRent, salary, toDollars, diner, october, upcoming, overdue)
    private var tagged = mapOf<TagId, List<TransactionId>>()

    // 1 USD = 0.5 EUR
    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { exchange(any(), any()) } answers {
            val rate = if (firstArg<ExchangeData>().fromCurrency == Some("USD")) 0.5 else 1.0
            Some(secondArg<BigDecimal>() * rate.toBigDecimal())
        }
    }

    private val exportCsvUseCase = mockk<ExportCsvUseCase>()
    private val toaster = mockk<Toaster>(relaxed = true)

    private val viewModel = ReportViewModel(
        dueTransactionsUseCase = mockk(relaxed = true),
        transactionRepository = mockk<TransactionRepository> {
            coEvery { findAll() } answers { transactions }
            coEvery { findByIds(any()) } answers {
                val ids = firstArg<List<TransactionId>>()
                transactions.filter { it.id in ids }
            }
        },
        periodState = PeriodState(FixedTime),
        exchangeUseCase = exchangeUseCase,
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns listOf(bank, dollars) },
        categoryRepository = mockk<CategoryRepository> { coEvery { findAll() } returns listOf(food, rent) },
        transactionsWithDateDividersUseCase = mockk<TransactionsWithDateDividersUseCase> {
            coEvery { withDateDividers(any(), any()) } returns emptyList()
        },
        transactionsIncomeExpenseUseCase = TransactionsIncomeExpenseUseCase(exchangeUseCase, TestDispatchersProvider),
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        tagRepository = mockk<TagRepository> {
            coEvery { findAll() } returns emptyList()
            coEvery { findByAllAssociatedIdForTagId(any()) } answers {
                firstArg<List<TagId>>().associateWith { tag ->
                    tagged[tag].orEmpty().map { TagAssociation(tag, AssociationId(it.value)) }
                }
            }
        },
        exportCsvUseCase = exportCsvUseCase,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        features = mockk(relaxed = true),
        resourceProvider = mockk<ResourceProvider> { every { getString(any()) } returns "Unspecified" },
        dispatchers = TestDispatchersProvider,
        toaster = toaster,
    )

    /** The report's own "Unspecified" category, for transactions without one. */
    private val unspecified: Category by lazy {
        var category: Category? = null
        viewModel.runTest { category = categories.first() }
        category!!
    }

    private fun filter(
        types: List<TransactionType> = listOf(TransactionType.INCOME, TransactionType.EXPENSE),
        period: TimePeriod? = TimePeriod(month = November, year = 2026),
        accounts: List<Account> = listOf(bank, dollars),
        categories: List<Category> = listOf(food, rent, unspecified),
        minAmount: Double? = null,
        maxAmount: Double? = null,
        includeKeywords: List<String> = emptyList(),
        excludeKeywords: List<String> = emptyList(),
        includedTags: List<TagId> = emptyList(),
        excludedTags: List<TagId> = emptyList(),
    ) = ReportFilter(
        trnTypes = types,
        period = period,
        accounts = accounts,
        categories = categories,
        currency = "EUR",
        minAmount = minAmount,
        maxAmount = maxAmount,
        includeKeywords = includeKeywords,
        excludeKeywords = excludeKeywords,
        includedTags = includedTags,
        excludedTags = excludedTags,
    )

    private fun report(vararg events: ReportScreenEvent, verify: ReportScreenState.() -> Unit) =
        viewModel.runTest(events = events.toList(), verify = verify)

    @Test
    fun `an export that can't write its file says so, and shares nothing`() {
        val file = mockk<Uri>()
        val screen = mockk<RootScreenContext>(relaxed = true) {
            every { createNewFile(any(), any()) } answers { secondArg<(Uri) -> Unit>().invoke(file) }
        }
        coEvery { exportCsvUseCase.exportToFile(file, any()) } returns FileSystem.Failure.IO(IOException()).left()

        report(ReportScreenEvent.OnFilter(filter()), ReportScreenEvent.OnExport(screen)) {}

        coVerify { toaster.show(R.string.msg_export_failed, any()) }
        verify(exactly = 0) { screen.shareCSVFile(any()) }
    }

    /** The activity, which Reports is given as a Context and uses as the RootScreen. */
    private abstract class RootScreenContext : Context(), RootScreen

    @Test
    fun `a report lists what was paid in the period, and adds it up`() {
        // when
        report(ReportScreenEvent.OnFilter(filter(accounts = listOf(bank)))) {
            // then
            transactions shouldContainExactlyInAnyOrder listOf(groceries, flatRent, salary)
            income shouldBe 1_000.0
            expenses shouldBe 120.0
            balance shouldBe 880.0
        }
    }

    @Test
    fun `what isn't paid yet is upcoming or overdue, outside the totals`() {
        // when
        report(ReportScreenEvent.OnFilter(filter(accounts = listOf(bank)))) {
            // then
            upcomingTransactions shouldBe listOf(upcoming)
            upcomingExpenses shouldBe 40.0
            overdueTransactions shouldBe listOf(overdue)
            overdueExpenses shouldBe 60.0
            expenses shouldBe 120.0
        }
    }

    @Test
    fun `a payment due earlier today is upcoming, as on Home and the account pages`() {
        // given: due this morning; it's noon
        val thisMorning = expense(bank, 25.0, "2026-11-15T08:00:00Z", food, settled = false)
        transactions = transactions + thisMorning

        // when
        report(ReportScreenEvent.OnFilter(filter(accounts = listOf(bank)))) {
            // then
            upcomingTransactions shouldBe listOf(thisMorning, upcoming)
            overdueTransactions shouldBe listOf(overdue)
        }
    }

    @Test
    fun `the account filter keeps transfers into the account as well as out of it`() {
        // when
        report(ReportScreenEvent.OnFilter(filter(types = TransactionType.entries, accounts = listOf(dollars)))) {
            // then
            transactions shouldContainExactlyInAnyOrder listOf(toDollars, diner)
        }
    }

    @Test
    fun `unspecified stands for transactions without a category, and transfers pass any category`() {
        // when
        report(
            ReportScreenEvent.OnFilter(
                filter(types = TransactionType.entries, accounts = listOf(bank), categories = listOf(unspecified))
            )
        ) {
            // then
            transactions shouldContainExactlyInAnyOrder listOf(salary, toDollars)
        }
    }

    @Test
    fun `amounts are compared in the base currency`() {
        // given 30 USD = 15 EUR
        report(ReportScreenEvent.OnFilter(filter(minAmount = 15.0, maxAmount = 20.0))) {
            // then
            transactions shouldContainExactlyInAnyOrder listOf(groceries, diner)
        }
    }

    @Test
    fun `keywords match the title or description in any case, and excluded ones win`() {
        // when
        report(ReportScreenEvent.OnFilter(filter(includeKeywords = listOf("rent", "DINER")))) {
            // then
            transactions shouldContainExactlyInAnyOrder listOf(flatRent, diner)
        }
        val withExcluded = filter(includeKeywords = listOf("rent", "diner"), excludeKeywords = listOf("November"))
        report(ReportScreenEvent.OnFilter(withExcluded)) {
            transactions shouldBe listOf(diner)
        }
    }

    @Test
    fun `tags keep only the transactions that have them, or leave them out`() {
        // given
        val holiday = TagId(UUID.randomUUID())
        tagged = mapOf(holiday to listOf(groceries.id, diner.id))

        // when
        report(ReportScreenEvent.OnFilter(filter(includedTags = listOf(holiday)))) {
            // then
            transactions shouldContainExactlyInAnyOrder listOf(groceries, diner)
        }
        report(ReportScreenEvent.OnFilter(filter(excludedTags = listOf(holiday)))) {
            transactions shouldContainExactlyInAnyOrder listOf(flatRent, salary)
        }
    }

    @Test
    fun `transfers count as income and expenses only when asked to`() {
        // given
        val withTransfers = filter(types = TransactionType.entries, accounts = listOf(bank))

        // when
        report(ReportScreenEvent.OnFilter(withTransfers)) {
            // then
            showTransfersAsIncExpCheckbox shouldBe true
            expenses shouldBe 120.0
            balance shouldBe 830.0
        }
        report(ReportScreenEvent.OnTreatTransfersAsIncomeExpense(true)) {
            expenses shouldBe 170.0
            balance shouldBe 830.0
        }
    }

    @Test
    fun `clearing the filter empties the report`() {
        // given
        report(ReportScreenEvent.OnFilter(filter())) {}

        // when
        report(ReportScreenEvent.OnFilter(null)) {
            // then
            filter shouldBe null
            transactions shouldBe emptyList()
            income shouldBe 0.0
            expenses shouldBe 0.0
        }
    }

    @Test
    fun `a filter without a period changes nothing`() {
        // when
        report(ReportScreenEvent.OnFilter(filter(period = null))) {
            // then
            filter shouldBe null
            transactions shouldBe emptyList()
        }
    }

    private fun expense(
        account: Account,
        amount: Double,
        time: String,
        category: Category?,
        title: String? = null,
        description: String? = null,
        settled: Boolean = true,
    ) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = title?.let(NotBlankTrimmedString::unsafe),
        description = description?.let(NotBlankTrimmedString::unsafe),
        category = category?.id,
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
        toValue = PositiveValue(PositiveDouble.unsafe(amount * 2), to.asset),
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")
        val November = Month.fromMonthValue(11)

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

        fun category(name: String) = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
        )
    }
}
