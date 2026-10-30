package com.ivy.domain.usecase.exchange

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.ivy.data.model.ExchangeRate
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Value
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NonZeroDouble
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.ExchangeRatesRepository
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExchangeUseCaseTest {
    private val exchangeRatesRepository = mockk<ExchangeRatesRepository>()
    private val currencyRepository = mockk<CurrencyRepository> {
        coEvery { getBaseCurrency() } returns BGN
    }
    private val useCase = ExchangeUseCase(exchangeRatesRepository, currencyRepository)

    init {
        // 1 BGN = 0.51 EUR = 0.55 USD; there's no rate for GBP
        coEvery { exchangeRatesRepository.findRate(any(), any()) } returns null
        coEvery { exchangeRatesRepository.findRate(BGN, EUR) } returns rate(EUR, 0.51)
        coEvery { exchangeRatesRepository.findRate(BGN, USD) } returns rate(USD, 0.55)
    }

    @Test
    fun `same currency is not converted`() = runTest {
        useCase.convert(positive(10.0, GBP), to = GBP) shouldBe Some(positive(10.0, GBP))
    }

    @Test
    fun `base currency to another one`() = runTest {
        useCase.convert(positive(100.0, BGN), to = EUR).amountOrNull().shouldNotBeNull() shouldBe
            (51.0 plusOrMinus Tolerance)
    }

    @Test
    fun `another currency to the base one`() = runTest {
        useCase.convert(positive(51.0, EUR), to = BGN).amountOrNull().shouldNotBeNull() shouldBe
            (100.0 plusOrMinus Tolerance)
    }

    @Test
    fun `between two other currencies through the base one`() = runTest {
        useCase.convert(positive(51.0, EUR), to = USD).amountOrNull().shouldNotBeNull() shouldBe
            (55.0 plusOrMinus Tolerance)
    }

    @Test
    fun `missing rate fails`() = runTest {
        useCase.convert(positive(10.0, GBP), to = BGN) shouldBe None
    }

    @Test
    fun `signed value keeps its sign`() = runTest {
        val res = useCase.convert(Value(NonZeroDouble.unsafe(-100.0), BGN), to = EUR)

        res.getOrNull()?.amount?.value.shouldNotBeNull() shouldBe (-51.0 plusOrMinus Tolerance)
    }

    @Test
    fun `sums values and reports the ones that failed`() = runTest {
        // when
        val res = useCase.convert(
            values = mapOf(
                BGN to NonZeroDouble.unsafe(100.0),
                EUR to NonZeroDouble.unsafe(51.0),
                GBP to NonZeroDouble.unsafe(5.0),
            ),
            to = BGN,
        )

        // then
        res.exchanged.getOrNull()?.amount?.value.shouldNotBeNull() shouldBe (200.0 plusOrMinus Tolerance)
        res.exchangeErrors shouldBe setOf(GBP)
    }

    @Test
    fun `nothing to sum is none`() = runTest {
        useCase.convert(values = emptyMap(), to = BGN) shouldBe ExchangeResult(None, emptySet())
    }

    private fun Option<PositiveValue>.amountOrNull(): Double? = getOrNull()?.amount?.value

    private fun positive(amount: Double, asset: AssetCode) =
        PositiveValue(PositiveDouble.unsafe(amount), asset)

    private fun rate(currency: AssetCode, rate: Double) = ExchangeRate(
        baseCurrency = BGN,
        currency = currency,
        rate = PositiveDouble.unsafe(rate),
        manualOverride = false,
    )

    companion object {
        const val Tolerance = 0.0001
        val BGN = AssetCode.unsafe("BGN")
        val EUR = AssetCode.unsafe("EUR")
        val USD = AssetCode.unsafe("USD")
        val GBP = AssetCode.unsafe("GBP")
    }
}
