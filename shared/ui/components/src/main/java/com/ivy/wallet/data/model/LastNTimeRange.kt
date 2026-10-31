package com.ivy.wallet.data.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Immutable
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.IntervalType
import com.ivy.data.model.incrementDate
import com.ivy.wallet.forDisplay
import java.time.Instant

@Suppress("DataClassFunctions")
@Immutable
data class LastNTimeRange(
    val periodN: Int,
    val periodType: IntervalType,
) {
    fun fromDate(
        timeProvider: TimeProvider
    ): Instant = periodType.incrementDate(
        date = timeProvider.utcNow(),
        intervalN = -periodN.toLong()
    )

    @Composable
    @ReadOnlyComposable
    fun forDisplay(): String =
        "$periodN ${periodType.forDisplay(periodN)}"
}
