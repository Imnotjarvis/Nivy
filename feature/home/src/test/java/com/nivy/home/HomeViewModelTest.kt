package com.nivy.home

import android.database.sqlite.SQLiteFullException
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.Toaster
import com.nivy.base.model.Theme
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.DataObserver
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.AppSettings
import com.nivy.domain.UserWrites
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.NotificationAccess
import com.nivy.domain.notification.NotificationList
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.PaymentNotificationListUseCase
import com.nivy.domain.notification.Stopped
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.home.usecase.Glance
import com.nivy.home.usecase.HistoryWithDateDividersUseCase
import com.nivy.home.usecase.HomeGlanceUseCase
import com.nivy.home.usecase.OverdueTransactions
import com.nivy.home.usecase.OverdueTransactionsUseCase
import com.nivy.home.usecase.UpcomingTransactions
import com.nivy.home.usecase.UpcomingTransactionsUseCase
import com.nivy.navigation.Navigation
import com.nivy.ui.R
import com.nivy.ui.testing.ComposeViewModelTest
import com.nivy.ui.testing.runComposeTest
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.ThemeState
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.data.TransactionHistoryDateDivider
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class HomeViewModelTest : ComposeViewModelTest() {
    private val monthsTransactions = persistentListOf<TransactionHistoryItem>(
        TransactionHistoryDateDivider(date = LocalDate.of(2026, 11, 6), income = 0.0, expenses = 12.5),
    )
    private val monthsTotals = IncomeExpensePair(income = BigDecimal("100"), expense = BigDecimal("40"))

    // The balance reads every transaction there is: it comes when this completes.
    private val balance = CompletableDeferred<BigDecimal>()
    private val syncExchangeRates = mockk<SyncExchangeRatesUseCase>(relaxed = true)
    private val dueTransactions = mockk<DueTransactionsUseCase>(relaxed = true)
    private val toaster = mockk<Toaster>(relaxed = true)
    private val historyWithDateDividers = mockk<HistoryWithDateDividersUseCase> {
        coEvery { history(any(), any()) } returns monthsTransactions
    }

    private val periodState = PeriodState(FixedTime)
    private val notificationList = mockk<PaymentNotificationListUseCase> {
        coEvery { load() } returns NotificationList(added = emptyList(), notAdded = emptyList())
    }
    private val notificationAccess = mockk<NotificationAccess>(relaxed = true) {
        every { granted() } returns false
        every { connected } returns MutableStateFlow(false)
        coEvery { stopped() } returns null
    }

    private val viewModel = HomeViewModel(
        periodState = periodState,
        themeState = ThemeState(),
        mainScreenState = mockk(relaxed = true),
        nav = Navigation(),
        dueTransactionsUseCase = dueTransactions,
        customerJourneyLogic = mockk { coEvery { loadCards() } returns emptyList() },
        historyWithDateDividersUseCase = historyWithDateDividers,
        walletIncomeExpenseUseCase = mockk<WalletIncomeExpenseUseCase> {
            coEvery { incomeExpense(any(), any(), any()) } returns monthsTotals
        },
        walletBalanceUseCase = mockk<WalletBalanceUseCase> {
            coEvery { balance(any(), any()) } coAnswers { balance.await() }
        },
        appPreferences = mockk<AppPreferences>(relaxed = true) {
            coEvery { settings() } returns AppSettings(
                theme = Theme.LIGHT,
                baseCurrency = "EUR",
                bufferAmount = 0.0,
                name = "",
            )
            coEvery { hideCurrentBalance() } returns false
            coEvery { hideIncome() } returns false
        },
        currencyRepository = mockk { coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR") },
        accountRepository = mockk { coEvery { findAll() } returns persistentListOf() },
        categoryRepository = mockk { coEvery { findAll() } returns emptyList() },
        upcomingTransactionsUseCase = mockk<UpcomingTransactionsUseCase> {
            coEvery { upcoming(any(), any()) } returns UpcomingTransactions(IncomeExpensePair.zero(), emptyList())
        },
        overdueTransactionsUseCase = mockk<OverdueTransactionsUseCase> {
            coEvery { overdue(any(), any()) } returns OverdueTransactions(IncomeExpensePair.zero(), emptyList())
        },
        homeGlanceUseCase = mockk<HomeGlanceUseCase> {
            coEvery { glance(baseCurrency = "EUR", accounts = any(), categories = any()) } returns TodaysGlance
        },
        transactionDao = mockk(relaxed = true),
        startDayOfMonthUseCase = mockk { coEvery { startDay() } returns 1 },
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        features = mockk(relaxed = true),
        syncExchangeRatesUseCase = syncExchangeRates,
        userWrites = UserWrites(toaster),
        dispatchers = TestDispatchersProvider,
        dataObserver = DataObserver(),
        notificationList = notificationList,
        notificationActions = mockk(relaxed = true),
        notificationAccess = notificationAccess,
        exchangeUseCase = mockk(),
    )

    @Test
    fun `the month's transactions and totals show before the balance is computed`() {
        viewModel.runTest {
            history shouldBe monthsTransactions
            historyLoaded shouldBe true
            stats shouldBe monthsTotals
            statsLoaded shouldBe true
            balanceLoaded shouldBe false
        }
    }

    @Test
    fun `the header's glance shows today, in the base currency, before the balance is computed`() {
        viewModel.runTest {
            glance shouldBe TodaysGlance
            balanceLoaded shouldBe false
        }
    }

    @Test
    fun `the balance shows once computed`() {
        balance.complete(BigDecimal("1234.50"))

        viewModel.runTest {
            balance shouldBe BigDecimal("1234.50")
            balanceLoaded shouldBe true
        }
    }

    @Test
    fun `a payment the database refuses shows that it failed, and Home stays`() {
        coEvery { dueTransactions.pay(any()) } throws SQLiteFullException("database or disk is full")

        viewModel.runTest(events = listOf(HomeEvent.PayOrGetPlanned(DueRent))) {
            history shouldBe monthsTransactions
        }

        coVerify { toaster.show(R.string.msg_change_not_saved) }
    }

    @Test
    fun `a period chosen for Home is the one the app's screens show`() {
        val january = TimePeriod(month = Month.monthsList()[0], year = 2027)

        viewModel.runTest(events = listOf(HomeEvent.SetPeriod(january))) {
            period shouldBe january
        }

        periodState.selected shouldBe january
    }

    @Test
    fun `the exchange rates don't sync while Home is loading`() {
        viewModel.runTest {
            balanceLoaded shouldBe false
        }

        coVerify(exactly = 0) { syncExchangeRates.syncIfDue(any()) }
    }

    @Test
    fun `the exchange rates sync, if due, once Home has loaded`() {
        balance.complete(BigDecimal("1234.50"))

        viewModel.runTest {
            balanceLoaded shouldBe true
        }

        coVerify(exactly = 1) { syncExchangeRates.syncIfDue(AssetCode.unsafe("EUR")) }
    }

    @Test
    fun `a month chosen after another shows its own transactions, even when the other one's load ends last`() {
        // December's transactions come slowly: January is chosen, and comes, before they do.
        val december = CompletableDeferred<Unit>()
        coEvery { historyWithDateDividers.history(any(), any()) } coAnswers {
            val month = firstArg<ClosedTimeRange>().from.atZone(ZoneId.of("UTC")).toLocalDate()
            if (month.monthValue == 12) december.await()
            persistentListOf(TransactionHistoryDateDivider(date = month, income = 0.0, expenses = 1.0))
        }

        runComposeTest(
            present = { viewModel.uiState() },
            actions = {
                viewModel.onEvent(HomeEvent.SelectNextMonth)
                viewModel.onEvent(HomeEvent.SelectNextMonth)
                december.complete(Unit)
            },
        ) {
            period.month?.monthValue shouldBe 1
            history shouldBe persistentListOf(
                TransactionHistoryDateDivider(date = LocalDate.of(2027, 1, 1), income = 0.0, expenses = 1.0)
            )
        }
    }

    @Test
    fun `the newest three payments added from notifications show, with all the list holds`() {
        val added = (1..4).map { listed(Outcome.Added, minutesAgo = it) }
        val notAdded = listed(Outcome.NotAdded, minutesAgo = 5)
        coEvery { notificationList.load() } returns NotificationList(added = added, notAdded = listOf(notAdded))

        viewModel.runTest {
            notifications?.latest shouldBe added.take(3)
            notifications?.count shouldBe 5
            notifications?.notAdded shouldBe 1
        }
    }

    @Test
    fun `Home says when Nivy stopped reading notifications`() {
        coEvery { notificationAccess.stopped() } returns Stopped(accessOff = true, since = null)

        viewModel.runTest {
            listeningStopped shouldBe Stopped(accessOff = true, since = null)
        }
    }

    private companion object {
        // A notification of the list, read some minutes before now; nothing it added is loaded.
        fun listed(outcome: Outcome, minutesAgo: Int): ListedNotification {
            val time = Instant.parse("2026-11-06T12:00:00Z").minusSeconds(60L * minutesAgo)
            return ListedNotification(
                notification = PaymentNotificationEntity(
                    id = UUID.randomUUID(),
                    packageName = "test.bank",
                    appName = "Bank",
                    notificationKey = "k$minutesAgo",
                    postTime = time,
                    title = "Bank",
                    text = "Hai pagato $minutesAgo € presso CONAD",
                    outcome = outcome.name,
                    why = null,
                    way = "Spent",
                    cents = 100L * minutesAgo,
                    currency = "EUR",
                    merchant = "CONAD",
                    card = null,
                    transactionId = null,
                    about = null,
                    aboutTime = null,
                    guessedAccount = false,
                    reviewed = false,
                ),
                outcome = outcome,
                why = null,
                transaction = null,
                alsoIn = emptyList(),
                since = null,
            )
        }

        // A planned payment due today.
        val DueRent = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("Rent"),
            description = null,
            category = null,
            time = Instant.parse("2026-11-06T09:00:00Z"),
            settled = false,
            metadata = TransactionMetadata(
                recurringRuleId = null,
                paidForDateTime = null,
                loanId = null,
                loanRecordId = null,
                purchaseDateTime = null,
            ),
            value = PositiveValue(PositiveDouble.unsafe(800.0), AssetCode.unsafe("EUR")),
            account = AccountId(UUID.randomUUID()),
        )

        val TodaysGlance = Glance(
            overdue = 2,
            spentToday = BigDecimal("12.40"),
            baseCurrency = "EUR",
            nextPayment = null,
        )

        /** November 6, 2026, noon, in UTC. */
        val FixedTime = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 6, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
