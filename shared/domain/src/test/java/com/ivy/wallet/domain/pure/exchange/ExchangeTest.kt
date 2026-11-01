package com.ivy.wallet.domain.pure.exchange

import arrow.core.Some
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExchangeTest {
    // Base EUR: 1 EUR = 1.08 USD = 27,900 VND
    private val rates = mapOf("USD" to 1.08, "VND" to 27_900.0)

    private suspend fun toEur(amount: Double, from: String): Double? = exchange(
        data = ExchangeData(baseCurrency = "EUR", fromCurrency = Some(from)),
        // As the callers convert amounts: Double.toBigDecimal() keeps Double's notation,
        // so 12,000,000.0 has scale -6 and 1000.0 has scale 1.
        amount = amount.toBigDecimal(),
        getExchangeRate = { _, currency -> rates[currency] },
    ).getOrNull()?.toDouble()

    @Test
    fun `a large whole amount keeps its value`() = runTest {
        toEur(12_000_000.0, from = "VND").shouldNotBeNull() shouldBe (430.11 plusOrMinus 0.01)
    }

    @Test
    fun `a whole amount keeps its cents`() = runTest {
        toEur(1000.0, from = "USD").shouldNotBeNull() shouldBe (925.926 plusOrMinus 0.001)
    }

    @Test
    fun `between two other currencies`() = runTest {
        val vnd = exchange(
            data = ExchangeData(baseCurrency = "EUR", fromCurrency = Some("USD"), toCurrency = "VND"),
            amount = 1000.0.toBigDecimal(),
            getExchangeRate = { _, currency -> rates[currency] },
        ).getOrNull().shouldNotBeNull().toDouble()

        vnd shouldBe (25_833_333.33 plusOrMinus 0.01)
    }
}
