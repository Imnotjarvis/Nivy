package com.nivy.data.repository

import com.nivy.base.time.TimeProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.remote.RemoteConsumerPricesDataSource
import java.time.Duration
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Italy's consumer prices, for amounts in euros after inflation: ISTAT's NIC index by month, as
 * built in and as downloaded since.
 */
@Singleton
class ConsumerPricesRepository @Inject constructor(
    private val remote: RemoteConsumerPricesDataSource,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
) {
    suspend fun prices(): ConsumerPrices = ConsumerPrices(BuiltInNic + appPreferences.consumerPrices())

    /**
     * Downloads the months missing, unless the index reaches the month before the current one,
     * which ISTAT publishes at its end, or a download was tried less than a day ago. True when the
     * index changed.
     */
    suspend fun syncIfDue(): Boolean {
        val prices = prices()
        val latest = prices.latest
        if (latest == null || !isDue(latest)) return false
        // The newest month again too: its index may have been provisional.
        val downloaded = remote.fetchNic(latest).getOrNull().orEmpty()
        appPreferences.setConsumerPricesTried(timeProvider.utcNow())
        val changed = downloaded.any { (month, value) -> prices.index[month] != value }
        if (changed) appPreferences.setConsumerPrices(appPreferences.consumerPrices() + downloaded)
        return changed
    }

    private suspend fun isDue(latest: YearMonth): Boolean {
        val now = timeProvider.utcNow()
        val triedAt = appPreferences.consumerPricesTriedAt()
        val missing = latest.isBefore(YearMonth.from(timeProvider.localDateNow()).minusMonths(1))
        // A time ahead of now means the clock was set back since: the day can't be counted from it.
        return missing && (triedAt == null || triedAt.isAfter(now) || Duration.between(triedAt, now) >= RetryAfter)
    }

    private companion object {
        val RetryAfter: Duration = Duration.ofDays(1)
    }
}

/** A consumer price index by month, to bring amounts of past months to the money of [latest]. */
data class ConsumerPrices(val index: Map<YearMonth, Double>) {
    // The newest month with an index.
    val latest: YearMonth? = index.keys.maxOrNull()

    /**
     * What an amount of [month] is multiplied by to be in [latest]'s money: 1 from [latest] on, as
     * no newer index tells otherwise, and null for a month without an index.
     */
    fun toLatest(month: YearMonth): Double? {
        val latest = latest ?: return null
        if (!month.isBefore(latest)) return 1.0
        val then = index[month] ?: return null
        return index.getValue(latest) / then
    }
}
