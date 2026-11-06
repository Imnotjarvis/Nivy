package com.ivy.data.preferences

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class ExchangeRatesSyncedTest {
    private val preferences = AppPreferences(InMemoryPreferencesDataStore())
    private val time = Instant.parse("2026-11-06T09:30:00Z")

    @Test
    fun `rates never downloaded have no time`() = runTest {
        preferences.exchangeRatesSyncedAt("USD") shouldBe null
    }

    @Test
    fun `a download is remembered for its base currency`() = runTest {
        preferences.setExchangeRatesSynced("USD", time)

        preferences.exchangeRatesSyncedAt("USD") shouldBe time
    }

    @Test
    fun `rates downloaded for another base currency don't count`() = runTest {
        preferences.setExchangeRatesSynced("USD", time)

        preferences.exchangeRatesSyncedAt("EUR") shouldBe null
    }
}
