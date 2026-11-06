package com.ivy.home

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.model.Theme
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.preferences.AppSettings
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.navigation.Navigation
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import com.ivy.wallet.PeriodState
import com.ivy.wallet.ThemeState
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import com.ivy.wallet.domain.action.viewmodel.home.OverdueAct
import com.ivy.wallet.domain.action.viewmodel.home.UpcomingAct
import com.ivy.wallet.domain.action.wallet.CalcIncomeExpenseAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HomeViewModelTest : ComposeViewModelTest() {
    private val monthsTransactions = persistentListOf<TransactionHistoryItem>(
        TransactionHistoryDateDivider(date = LocalDate.of(2026, 11, 6), income = 0.0, expenses = 12.5),
    )
    private val monthsTotals = IncomeExpensePair(income = BigDecimal("100"), expense = BigDecimal("40"))

    // The balance reads every transaction there is: it comes when this completes.
    private val balance = CompletableDeferred<BigDecimal>()
    private val syncExchangeRates = mockk<SyncExchangeRatesUseCase>(relaxed = true)

    private val viewModel = HomeViewModel(
        periodState = PeriodState(FixedTime),
        themeState = ThemeState(),
        mainScreenState = mockk(relaxed = true),
        nav = Navigation(),
        dueTransactionsUseCase = mockk(relaxed = true),
        customerJourneyLogic = mockk { coEvery { loadCards() } returns emptyList() },
        historyWithDateDivsAct = mockk<HistoryWithDateDivsAct> {
            coEvery { this@mockk(any()) } returns monthsTransactions
        },
        calcIncomeExpenseAct = mockk<CalcIncomeExpenseAct> { coEvery { this@mockk(any()) } returns monthsTotals },
        calcWalletBalanceAct = mockk<CalcWalletBalanceAct> {
            coEvery { this@mockk(any()) } coAnswers { balance.await() }
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
        accountsAct = mockk { coEvery { this@mockk() } returns persistentListOf() },
        categoryRepository = mockk { coEvery { findAll() } returns emptyList() },
        upcomingAct = mockk<UpcomingAct> {
            coEvery { this@mockk(any()) } returns UpcomingAct.Output(IncomeExpensePair.zero(), emptyList())
        },
        overdueAct = mockk<OverdueAct> {
            coEvery { this@mockk(any()) } returns OverdueAct.Output(IncomeExpensePair.zero(), emptyList())
        },
        transactionDao = mockk(relaxed = true),
        startDayOfMonthAct = mockk { coEvery { this@mockk() } returns 1 },
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        features = mockk(relaxed = true),
        syncExchangeRatesUseCase = syncExchangeRates,
        dispatchers = TestDispatchersProvider,
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
    fun `the balance shows once computed`() {
        balance.complete(BigDecimal("1234.50"))

        viewModel.runTest {
            balance shouldBe BigDecimal("1234.50")
            balanceLoaded shouldBe true
        }
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

    private companion object {
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
