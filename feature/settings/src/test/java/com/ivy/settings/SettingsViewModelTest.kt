package com.ivy.settings

import arrow.core.None
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.ivy.base.Toaster
import com.ivy.data.model.Budget
import com.ivy.data.model.BudgetId
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.BudgetRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.ui.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val budgetRepository = mockk<BudgetRepository>(relaxUnitFun = true)
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val exchangeUseCase = mockk<ExchangeUseCase>()
    private val syncExchangeRates = mockk<SyncExchangeRatesUseCase> {
        coEvery { sync(any()) } returns Unit.right()
    }
    private val toaster = mockk<Toaster>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SettingsViewModel(
            currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
            themeState = mockk(relaxed = true),
            deleteAllDataUseCase = mockk(relaxed = true),
            nav = mockk(relaxed = true),
            appPreferences = appPreferences,
            backupDataUseCase = mockk(relaxed = true),
            startDayOfMonthAct = mockk(relaxed = true),
            updateStartDayOfMonthAct = mockk(relaxed = true),
            syncExchangeRatesUseCase = syncExchangeRates,
            budgetRepository = budgetRepository,
            exchangeUseCase = exchangeUseCase,
            exportCsvUseCase = mockk(relaxed = true),
            context = mockk(relaxed = true),
            timeProvider = mockk(relaxed = true),
            toaster = toaster,
            dateFormatState = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `budgets follow the base currency to the new one`() {
        val budgets = listOf(budget(100.0), budget(40.0))
        coEvery { budgetRepository.findAll() } returns budgets
        // 1 EUR = 2 USD
        coEvery { exchangeUseCase.convert(any<PositiveValue>(), Usd) } answers {
            Some(PositiveValue(PositiveDouble.unsafe(firstArg<PositiveValue>().amount.value * 2), Usd))
        }

        viewModel.onEvent(SettingsEvent.SetCurrency("USD"))

        coVerify(timeout = Timeout) { syncExchangeRates.sync(Usd) }
        // Converted with the rates of the old base currency, before it changes.
        coVerifyOrder {
            budgetRepository.saveMany(
                listOf(
                    budgets[0].copy(amount = PositiveDouble.unsafe(200.0)),
                    budgets[1].copy(amount = PositiveDouble.unsafe(80.0)),
                )
            )
            appPreferences.setBaseCurrency("USD")
        }
    }

    @Test
    fun `budgets without a rate to the new currency keep their amounts`() {
        coEvery { budgetRepository.findAll() } returns listOf(budget(100.0))
        coEvery { exchangeUseCase.convert(any<PositiveValue>(), Usd) } returns None

        viewModel.onEvent(SettingsEvent.SetCurrency("USD"))

        coVerify(timeout = Timeout) { syncExchangeRates.sync(Usd) }
        coVerify { budgetRepository.saveMany(emptyList()) }
        coVerify { appPreferences.setBaseCurrency("USD") }
    }

    @Test
    fun `choosing the current currency again changes no budget`() {
        coEvery { budgetRepository.findAll() } returns listOf(budget(100.0))

        viewModel.onEvent(SettingsEvent.SetCurrency("EUR"))

        coVerify(timeout = Timeout) { syncExchangeRates.sync(Eur) }
        coVerify(exactly = 0) { budgetRepository.saveMany(any()) }
    }

    @Test
    fun `rates that can't be downloaded for the new currency are reported`() {
        coEvery { budgetRepository.findAll() } returns emptyList()
        coEvery { syncExchangeRates.sync(Usd) } returns "No connection".left()

        viewModel.onEvent(SettingsEvent.SetCurrency("USD"))

        coVerify(timeout = Timeout) { toaster.show(R.string.msg_exchange_rates_not_downloaded, any()) }
    }

    private fun budget(amount: Double) = Budget(
        id = BudgetId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Budget"),
        amount = PositiveDouble.unsafe(amount),
        categories = emptyList(),
        accounts = emptyList(),
        orderNum = 0.0,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

        // The currency change runs on the IO dispatcher.
        const val Timeout = 5_000L
    }
}
