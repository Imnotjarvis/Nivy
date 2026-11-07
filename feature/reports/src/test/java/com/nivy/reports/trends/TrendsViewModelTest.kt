package com.nivy.reports.trends

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.wallet.WalletBalanceUseCase
import com.nivy.ui.testing.runTest
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class TrendsViewModelTest {

    private val bank = account("Bank", includeInBalance = true)
    private val savings = account("Savings", includeInBalance = false)

    // What the net worth is asked of: the time it's at, and whether every account counts. It
    // answers with that time's month number.
    private val asked = mutableListOf<Pair<Instant, Boolean>>()
    private val walletBalanceUseCase = mockk<WalletBalanceUseCase> {
        coEvery { balance(any(), any(), any(), any()) } answers {
            val at = thirdArg<ClosedTimeRange>().to
            asked += at to arg<Boolean>(3)
            BigDecimal(at.atOffset(ZoneOffset.UTC).monthValue)
        }
    }

    private val viewModel = TrendsViewModel(
        transactionRepository = mockk<TransactionRepository> {
            coEvery { findAllBetween(any(), any()) } returns listOf(
                income(bank, 1_000.0, "2026-11-05T10:00:00Z"),
                income(savings, 500.0, "2026-11-06T10:00:00Z"),
            )
        },
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns listOf(bank, savings) },
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        exchangeUseCase = ExchangeUseCase(
            exchangeRatesRepository = mockk(),
            currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
            dispatchers = TestDispatchersProvider,
        ),
        walletBalanceUseCase = walletBalanceUseCase,
        appPreferences = mockk<AppPreferences> { coEvery { startDayOfMonth() } returns 1 },
        timeProvider = FixedTime,
        timeConverter = TestTimeConverter(),
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `net worth counts every account, at each month's end and now for the current month`() {
        viewModel.runTest {
            // December 2025 to November 2026
            months.map { it.netWorth } shouldBe listOf(12.0) + (1..11).map { it.toDouble() }
            asked.all { (_, everyAccount) -> everyAccount } shouldBe true
            asked.first().first.atOffset(ZoneOffset.UTC).toLocalDate() shouldBe LocalDate.of(2025, 12, 31)
            asked.last().first shouldBe FixedTime.utcNow()
        }
    }

    @Test
    fun `income counts only the accounts included in the balance`() {
        viewModel.runTest {
            months.last().income shouldBe 1_000.0
        }
    }

    private fun income(account: Account, amount: Double, time: String) = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
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

    private fun account(name: String, includeInBalance: Boolean) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = Eur,
        color = ColorInt(0),
        icon = null,
        includeInBalance = includeInBalance,
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
