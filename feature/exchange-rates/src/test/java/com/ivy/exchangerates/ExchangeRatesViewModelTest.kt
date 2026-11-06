package com.ivy.exchangerates

import arrow.core.left
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.Toaster
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.ExchangeRatesRepository
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.ui.R
import com.ivy.ui.testing.runTest
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
