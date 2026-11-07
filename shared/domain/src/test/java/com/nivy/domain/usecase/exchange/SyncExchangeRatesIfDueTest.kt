package com.nivy.domain.usecase.exchange

import arrow.core.left
import arrow.core.right
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.ExchangeRatesRepository
import io.kotest.assertions.arrow.core.shouldBeRight
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SyncExchangeRatesIfDueTest {
    private val repository = mockk<ExchangeRatesRepository>(relaxed = true) {
        coEvery { fetchEurExchangeRates() } returns "offline".left()
    }
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val timeProvider = mockk<TimeProvider> { every { utcNow() } returns Now }
    private val useCase = SyncExchangeRatesUseCase(repository, appPreferences, timeProvider)

    @Test
    fun `downloads the rates when they never were`() = runTest {
        syncedAt(null)

        useCase.syncIfDue(AssetCode.USD)

        coVerify(exactly = 1) { repository.fetchEurExchangeRates() }
    }

    @Test
    fun `doesn't download them again within a day`() = runTest {
        syncedAt(Now - Duration.ofHours(23))

        useCase.syncIfDue(AssetCode.USD).shouldBeRight()

        coVerify(exactly = 0) { repository.fetchEurExchangeRates() }
    }

    @Test
    fun `downloads them again once a day has passed`() = runTest {
        syncedAt(Now - Duration.ofDays(1))

        useCase.syncIfDue(AssetCode.USD)

        coVerify(exactly = 1) { repository.fetchEurExchangeRates() }
    }

    @Test
    fun `downloads them when the clock was set back since`() = runTest {
        syncedAt(Now + Duration.ofHours(1))

        useCase.syncIfDue(AssetCode.USD)

        coVerify(exactly = 1) { repository.fetchEurExchangeRates() }
    }

    @Test
    fun `a download is remembered once the rates are saved`() = runTest {
        coEvery { repository.fetchEurExchangeRates() } returns listOf(
            ExchangeRate(AssetCode.EUR, AssetCode.USD, PositiveDouble.unsafe(1.08), manualOverride = false),
            ExchangeRate(AssetCode.EUR, AssetCode.GBP, PositiveDouble.unsafe(0.86), manualOverride = false),
        ).right()

        useCase.sync(AssetCode.USD).shouldBeRight()

        coVerify(exactly = 1) { repository.saveManyRates(any()) }
        coVerify(exactly = 1) { appPreferences.setExchangeRatesSynced("USD", Now) }
    }

    @Test
    fun `a failed download isn't remembered`() = runTest {
        useCase.sync(AssetCode.USD)

        coVerify(exactly = 0) { appPreferences.setExchangeRatesSynced(any(), any()) }
    }

    private fun syncedAt(time: Instant?) {
        coEvery { appPreferences.exchangeRatesSyncedAt("USD") } returns time
    }

    private companion object {
        val Now: Instant = Instant.parse("2026-11-06T09:30:00Z")
    }
}
