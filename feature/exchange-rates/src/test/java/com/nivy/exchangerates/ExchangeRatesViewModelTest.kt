package com.nivy.exchangerates

import arrow.core.left
import com.nivy.base.TestDispatchersProvider
import com.nivy.base.Toaster
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.nivy.ui.R
import com.nivy.ui.testing.runTest
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class ExchangeRatesViewModelTest {
    private val toaster = mockk<Toaster>(relaxed = true)

    @Test
    fun `rates that can't be downloaded when the screen opens are reported`() {
        val viewModel = ExchangeRatesViewModel(
            syncExchangeRatesUseCase = mockk<SyncExchangeRatesUseCase> {
                coEvery { sync(any()) } returns "No connection".left()
            },
            currencyRepo = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
            exchangeRatesRepo = mockk<ExchangeRatesRepository> { every { findAll() } returns flowOf(emptyList()) },
            dispatchers = TestDispatchersProvider,
            toaster = toaster,
        )

        viewModel.runTest {
            baseCurrency shouldBe "EUR"
        }

        coVerify { toaster.show(R.string.msg_exchange_rates_not_downloaded, any()) }
    }

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
    }
}
