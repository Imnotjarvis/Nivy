package com.nivy.widget.balance

import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.domain.usecase.wallet.WalletIncomeExpenseUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WalletBalanceWidgetNumbersTest {
    @Test
    fun `the widget shows the balance, and the income and expenses of the month from the person's start day`() =
        runTest {
            // given: the month starts on the 15th, and today is November 20th
            val month = slot<ClosedTimeRange>()
            val timeConverter = TestTimeConverter()
            val numbers = WalletBalanceWidgetNumbers(
                appPreferences = mockk<AppPreferences> {
                    coEvery { startDayOfMonth() } returns 15
                    coEvery { isAppLockEnabled() } returns false
                },
                currencyRepository = mockk<CurrencyRepository> {
                    coEvery { getBaseCurrency() } returns AssetCode.unsafe("EUR")
                },
                walletBalanceUseCase = mockk<WalletBalanceUseCase> {
                    coEvery { balance(baseCurrency = "EUR") } returns BigDecimal("2419.50")
                },
                accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns emptyList() },
                walletIncomeExpenseUseCase = mockk<WalletIncomeExpenseUseCase> {
                    coEvery {
                        incomeExpense(baseCurrency = "EUR", accounts = emptyList(), range = capture(month))
                    } returns IncomeExpensePair(income = BigDecimal("2500"), expense = BigDecimal("80.50"))
                },
                timeProvider = November20,
                timeConverter = timeConverter,
            )

            // when
            val shown = numbers.load()

            // then
            shown shouldBe WalletBalanceWidgetNumbers.Shown(
                appLocked = false,
                currency = "EUR",
                balance = 2_419.5,
                income = 2_500.0,
                expense = 80.5,
            )
            with(timeConverter) {
                month.captured.from.toLocalDate() shouldBe LocalDate.of(2026, 11, 15)
                month.captured.to.toLocalDate() shouldBe LocalDate.of(2026, 12, 14)
            }
        }

    private companion object {
        /** November 20, 2026, noon, in UTC. */
        val November20 = object : TimeProvider {
            private val now = LocalDateTime.of(2026, 11, 20, 12, 0)
            override fun getZoneId(): ZoneId = ZoneId.of("UTC")
            override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
            override fun localNow(): LocalDateTime = now
            override fun localDateNow(): LocalDate = now.toLocalDate()
            override fun localTimeNow(): LocalTime = now.toLocalTime()
        }
    }
}
