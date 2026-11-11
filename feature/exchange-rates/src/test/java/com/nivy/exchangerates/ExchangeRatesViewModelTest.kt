package com.nivy.exchangerates

import android.database.sqlite.SQLiteFullException
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.Toaster
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.exchangerates.data.RateUi
import com.nivy.ui.R
import com.nivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class ExchangeRatesViewModelTest {
    private val toaster = mockk<Toaster>(relaxed = true)
    private var downloaded: Either<String, Unit> = Unit.right()
    private val syncExchangeRates = mockk<SyncExchangeRatesUseCase> {
        coEvery { sync(any()) } answers { downloaded }
    }
    private val exchangeRatesRepo = mockk<ExchangeRatesRepository>(relaxUnitFun = true) {
        every { findAll() } returns flowOf(
            listOf(
                rate(Eur, "USD", 1.08),
                rate(Eur, "GBP", 0.85, byHand = true),
                rate(Eur, "BGN", 1.95),
                // Of another base currency: not this wallet's.
                rate(AssetCode.unsafe("USD"), "EUR", 0.92),
            )
        )
    }

    private val viewModel = ExchangeRatesViewModel(
        syncExchangeRatesUseCase = syncExchangeRates,
        currencyRepo = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
        exchangeRatesRepo = exchangeRatesRepo,
        dispatchers = TestDispatchersProvider,
        toaster = toaster,
        userWrites = UserWrites(toaster),
    )

    @Test
    fun `the base currency's rates are listed, those entered by hand apart from the downloaded`() {
        viewModel.runTest {
            baseCurrency shouldBe "EUR"
            manual shouldBe listOf(RateUi("EUR", "GBP", 0.85))
            automatic shouldBe listOf(RateUi("EUR", "USD", 1.08), RateUi("EUR", "BGN", 1.95))
        }
    }

    @Test
    fun `a search lists the currencies whose code holds it, whatever the case`() {
        viewModel.runTest(events = listOf(RatesEvent.Search(" gb "))) {
            manual shouldBe listOf(RateUi("EUR", "GBP", 0.85))
            automatic shouldBe emptyList()
        }
    }

    @Test
    fun `rates that can't be downloaded when the screen opens are reported`() {
        downloaded = "No connection".left()

        viewModel.runTest {
            baseCurrency shouldBe "EUR"
        }

        coVerify { toaster.show(R.string.msg_exchange_rates_not_downloaded, any()) }
    }

    @Test
    fun `a rate added and a rate changed are saved as entered by hand`() {
        viewModel.runTest(
            events = listOf(
                RatesEvent.AddRate(RateUi("EUR", "CHF", 0.94)),
                RatesEvent.UpdateRate(RateUi("EUR", "USD", 1.08), newRate = 1.1),
            )
        ) {}

        coVerifyOrder {
            exchangeRatesRepo.save(rate(Eur, "CHF", 0.94, byHand = true))
            exchangeRatesRepo.save(rate(Eur, "USD", 1.1, byHand = true))
        }
        coVerify(exactly = 0) { toaster.show(R.string.msg_rate_not_saved, any()) }
    }

    @Test
    fun `a rate that can't be saved says so in words, not in the validator's`() {
        viewModel.runTest(events = listOf(RatesEvent.UpdateRate(RateUi("EUR", "USD", 1.08), newRate = 0.0))) {}

        coVerify { toaster.show(R.string.msg_rate_not_saved, any()) }
        coVerify(exactly = 0) { toaster.show(any<String>(), any()) }
        coVerify(exactly = 0) { exchangeRatesRepo.save(any()) }
    }

    @Test
    fun `a rate the database refuses says so`() {
        coEvery { exchangeRatesRepo.save(any()) } throws SQLiteFullException("database or disk is full")

        viewModel.runTest(events = listOf(RatesEvent.AddRate(RateUi("EUR", "CHF", 0.94)))) {}

        coVerify(exactly = 1) { toaster.show(R.string.msg_rate_not_saved, any()) }
    }

    @Test
    fun `removing a rate entered by hand deletes it, then downloads the real one`() {
        viewModel.runTest(events = listOf(RatesEvent.RemoveOverride(RateUi("EUR", "GBP", 0.85)))) {}

        coVerifyOrder {
            // when the screen opens
            syncExchangeRates.sync(Eur)
            exchangeRatesRepo.deleteByBaseCurrencyAndCurrency(baseCurrency = Eur, currency = AssetCode.unsafe("GBP"))
            syncExchangeRates.sync(Eur)
        }
    }

    @Test
    fun `a removal the database refuses says so, and downloads nothing more`() {
        coEvery { exchangeRatesRepo.deleteByBaseCurrencyAndCurrency(any(), any()) } throws
            SQLiteFullException("database or disk is full")

        viewModel.runTest(events = listOf(RatesEvent.RemoveOverride(RateUi("EUR", "GBP", 0.85)))) {}

        coVerify(exactly = 1) { toaster.show(R.string.msg_rate_not_removed, any()) }
        // Once, when the screen opened.
        coVerify(exactly = 1) { syncExchangeRates.sync(any()) }
    }

    private companion object {
        val Eur = AssetCode.unsafe("EUR")

        fun rate(base: AssetCode, currency: String, rate: Double, byHand: Boolean = false) = ExchangeRate(
            baseCurrency = base,
            currency = AssetCode.unsafe(currency),
            rate = PositiveDouble.unsafe(rate),
            manualOverride = byHand,
        )
    }
}
