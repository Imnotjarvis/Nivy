package com.nivy.loans.loan

import arrow.core.Some
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.model.LoanType
import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NonZeroDouble
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.Value
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.ui.testing.runTest
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class LoanViewModelTest {

    private val bank = account("Bank", Eur, archived = false)
    private val oldDollars = account("Old dollars", Usd, archived = true)

    // 100 USD borrowed on an account archived since; 1 USD = 0.5 EUR.
    private val loan = Loan(
        id = LoanId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Car"),
        amount = PositiveDouble.unsafe(100.0),
        type = LoanType.BORROW,
        color = ColorInt(0),
        icon = null,
        account = oldDollars.id,
        note = null,
        dateTime = null,
        orderNum = 0.0,
    )

    private val viewModel = LoanViewModel(
        loanRepository = mockk<LoanRepository> {
            coEvery { findAll() } returns listOf(loan)
            coEvery { findRecordsByLoanId(loan.id) } returns emptyList()
        },
        currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        appPreferences = mockk<AppPreferences> { coEvery { lastSelectedAccountId() } returns null },
        accountCreator = mockk(relaxed = true),
        loanTransactionsUseCase = mockk(relaxed = true),
        exchangeUseCase = mockk<ExchangeUseCase> {
            coEvery { convert(any<Value>(), any()) } answers { Some(firstArg()) }
            coEvery { convert(Value(NonZeroDouble.unsafe(100.0), Usd), to = Eur) } returns
                Some(Value(NonZeroDouble.unsafe(50.0), Eur))
        },
        accountRepository = mockk<AccountRepository> { coEvery { findAll() } returns listOf(bank, oldDollars) },
        timeConverter = TestTimeConverter(),
        timeProvider = FixedTime,
        dateTimePicker = mockk(relaxed = true),
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
    fun `a loan on an archived account is shown in its account's currency, and totalled converted`() {
        viewModel.runTest {
            pendingLoans.single().currencyCode shouldBe "USD"
            totalOweAmount shouldBe "${50.0.format("EUR")} EUR"
        }
    }

    private fun account(name: String, asset: AssetCode, archived: Boolean) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = asset,
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = archived,
        orderNum = 0.0,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

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
