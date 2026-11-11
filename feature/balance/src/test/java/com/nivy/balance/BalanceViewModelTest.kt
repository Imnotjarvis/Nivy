package com.nivy.balance

import com.nivy.balance.detail.MonthDetail
import com.nivy.balance.detail.MonthDetailUseCase
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.usecase.planned.DueTransactionsUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.ui.testing.runTest
import com.nivy.wallet.PeriodState
import com.nivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class BalanceViewModelTest {

    // 1,000 now; by the end of November, a card purchase of 300 is charged, and 100 more is due.
    private val walletBalanceUseCase = mockk<WalletBalanceUseCase> {
        coEvery { balance(any(), isNull()) } returns BigDecimal(1_000)
        coEvery { balance(any(), isNull(inverse = true)) } returns BigDecimal(700)
    }

    private val viewModel = BalanceViewModel(
        dueTransactionsUseCase = mockk<DueTransactionsUseCase> {
            coEvery { dueBalance(any(), any()) } returns -100.0
        },
        periodState = PeriodState(FixedTime),
        currencyRepository = mockk<CurrencyRepository> {
            coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR")
        },
        walletBalanceUseCase = walletBalanceUseCase,
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        monthDetailUseCase = mockk<MonthDetailUseCase> {
            coEvery { detail(any(), any(), any()) } returns MonthDetail.Empty
        },
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
    fun `the balance after planned payments counts the card purchases charged by the period's end`() {
        viewModel.runTest {
            currentBalance shouldBe 1_000.0
            plannedPaymentsAmount shouldBe -400.0
            balanceAfterPlannedPayments shouldBe 600.0
        }
    }

    @Test
    fun `the month in detail loads under the balances`() {
        viewModel.runTest {
            detail shouldBe MonthDetail.Empty
        }
    }

    @Test
    fun `a period that has ended adds no charges to come`() {
        viewModel.runTest(events = listOf(BalanceEvent.OnPreviousMonth)) {
            plannedPaymentsAmount shouldBe -100.0
            balanceAfterPlannedPayments shouldBe 900.0
        }
    }

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
