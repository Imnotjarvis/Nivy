package com.nivy.wallet.data.model

import androidx.compose.runtime.Immutable
import com.nivy.base.time.TimeProvider
import com.nivy.wallet.utils.nivyMinTime
import com.nivy.wallet.utils.nivyMaxTime
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.transaction.startOfToday
import java.time.Instant

@Suppress("DataClassFunctions")
@Immutable
data class FromToTimeRange(
    val from: Instant?,
    val to: Instant?,
) {
    fun from(): Instant =
        from ?: nivyMinTime()

    fun to(): Instant =
        to ?: nivyMaxTime()

    fun upcomingFrom(
        timeProvider: TimeProvider
    ): Instant {
        val startOfToday = startOfToday(timeProvider)
        return if (includes(startOfToday)) startOfToday else from()
    }

    fun overdueTo(
        timeProvider: TimeProvider
    ): Instant {
        val startOfToday = startOfToday(timeProvider)
        return if (includes(startOfToday)) startOfToday else to()
    }

    /** Both ends included: a period's first and last moments belong to it. */
    fun includes(dateTime: Instant): Boolean =
        !dateTime.isBefore(from()) && !dateTime.isAfter(to())

    fun toDisplay(
        timeFormatter: TimeFormatter
    ): String = with(timeFormatter) {
        val style = TimeFormatter.Style.DateOnly(includeWeekDay = false)
        when {
            from != null && to != null -> {
                "${from.formatLocal(style)} - ${to.formatLocal(style)}"
            }

            from != null && to == null -> {
                "From ${from.formatLocal(style)}"
            }

            from == null && to != null -> {
                "To ${to.formatLocal(style)}"
            }

            else -> {
                "Range"
            }
        }
    }
}

fun FromToTimeRange.toCloseTimeRange(): ClosedTimeRange {
    return ClosedTimeRange(
        from = from ?: nivyMinTime(),
        to = to ?: nivyMaxTime()
    )
}

fun FromToTimeRange.toUTCCloseTimeRange(): ClosedTimeRange {
    return ClosedTimeRange(
        from = from ?: nivyMinTime(),
        to = to ?: nivyMaxTime()
    )
}
