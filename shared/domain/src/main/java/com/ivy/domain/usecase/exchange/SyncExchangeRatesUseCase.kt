package com.ivy.domain.usecase.exchange

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.ExchangeRate
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.ExchangeRatesRepository
import timber.log.Timber
import java.time.Duration
import javax.inject.Inject

class SyncExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRatesRepository,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
) {
    /**
     * Downloads the rates unless they were downloaded for [baseCurrency] less than a day ago: they
     * change once a day, and the app used to download them on every launch.
     */
    suspend fun syncIfDue(baseCurrency: AssetCode): Either<String, Unit> {
        val syncedAt = appPreferences.exchangeRatesSyncedAt(baseCurrency.code)
        val now = timeProvider.utcNow()
        // A time ahead of now means the clock was set back since: the day can't be counted from it.
        val due = syncedAt == null || syncedAt.isAfter(now) || Duration.between(syncedAt, now) >= SyncInterval
        return if (due) sync(baseCurrency) else Unit.right()
    }

    suspend fun sync(baseCurrency: AssetCode): Either<String, Unit> = either {
        val eurRates = repository.fetchEurExchangeRates().bind()
            .associateBy(ExchangeRate::currency)
            .mapValues { it.value.rate }

        /* At this point we must have non-empty EUR rates
           Now we must convert them to base currency

        "eur": {
            "bgn": 1.955902,
            "usd": 1.062366,
        }
         */
        val eurToBaseRate = eurRates[baseCurrency]
        ensure(eurToBaseRate != null) { "EUR to ${baseCurrency.code} rate not found." }

        val baseCurrencyRates = eurRates.mapNotNull { (target, rate) ->
            either {
                val baseTargetRate = rate.value / eurToBaseRate.value
                ExchangeRate(
                    baseCurrency = baseCurrency,
                    currency = target,
                    rate = PositiveDouble.from(baseTargetRate).bind(),
                    manualOverride = false
                )
            }.getOrNull()
        }.toList()
        Timber.d("Updating %d exchange rates", baseCurrencyRates.size)
        val manuallyOverridden = repository.findAllManuallyOverridden()
            .map { it.identifier() }
            .toSet()
        val newRatesToSave = baseCurrencyRates.mapNotNull { newRate ->
            val hasManualOverride = newRate.identifier() in manuallyOverridden
            // Only save exchange rates that are not overridden
            newRate.takeIf { !hasManualOverride }
        }
        repository.saveManyRates(newRatesToSave)
        appPreferences.setExchangeRatesSynced(baseCurrency.code, timeProvider.utcNow())
    }

    private fun ExchangeRate.identifier(): AssetCodeId = AssetCodeId(
        baseCurrency = baseCurrency,
        currency = currency
    )

    data class AssetCodeId(
        val baseCurrency: AssetCode,
        val currency: AssetCode,
    )

    private companion object {
        val SyncInterval: Duration = Duration.ofDays(1)
    }
}
