package com.nivy.exchangerates.modal

import com.nivy.exchangerates.data.RateUi
import io.kotest.matchers.shouldBe
import org.junit.Test

class AddableRateTest {
    @Test
    fun `a known currency at a rate above 0 can be added, its code as the app writes it`() {
        addableRate(baseCurrency = "EUR", currency = " usd ", rate = 1.08) shouldBe RateUi("EUR", "USD", 1.08)
    }

    @Test
    fun `nothing can be added without a rate above 0`() {
        addableRate(baseCurrency = "EUR", currency = "USD", rate = null) shouldBe null
        addableRate(baseCurrency = "EUR", currency = "USD", rate = 0.0) shouldBe null
    }

    @Test
    fun `nor for a currency the app doesn't know, or for the base currency itself`() {
        addableRate(baseCurrency = "EUR", currency = "Dollars", rate = 1.08) shouldBe null
        addableRate(baseCurrency = "EUR", currency = "eur", rate = 1.0) shouldBe null
    }
}
