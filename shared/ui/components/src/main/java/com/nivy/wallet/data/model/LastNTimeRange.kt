package com.nivy.wallet.data.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Immutable
import com.nivy.base.time.TimeProvider
import com.nivy.base.model.IntervalType
import com.nivy.base.model.incrementDate
import com.nivy.wallet.forDisplay
import java.time.Instant

@Immutable
data class LastNTimeRange(
    val periodN: Int,
    val periodType: IntervalType,
) {
    fun fromDate(
        timeProvider: TimeProvider
    ): Instant = periodType.incrementDate(
        date = timeProvider.utcNow(),
        intervalN = -periodN.toLong(),
        zone = timeProvider.getZoneId(),
    )

    @Composable
    @ReadOnlyComposable
    fun forDisplay(): String =
        "$periodN ${periodType.forDisplay(periodN)}"
}
