package com.nivy.wallet.utils

import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class GainPercentTest {
    private val defaultLocale = Locale.getDefault()

    @Before
    fun usLocale() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `a gain is a signed share of the money put in`() {
        formatGainPercent(gain = 520.0, invested = 8_420.0) shouldBe "+6.2%"
        formatGainPercent(gain = -120.0, invested = 1_000.0) shouldBe "-12.0%"
        formatGainPercent(gain = 0.0, invested = 1_000.0) shouldBe "0.0%"
    }

    @Test
    fun `without money put in there's no share`() {
        formatGainPercent(gain = 50.0, invested = 0.0) shouldBe null
        formatGainPercent(gain = 50.0, invested = -10.0) shouldBe null
    }
}
