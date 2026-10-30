package com.ivy.legacy.data.model

import androidx.compose.runtime.Immutable
import com.ivy.base.time.TimeProvider
import com.ivy.legacy.utils.ivyMinTime
import com.ivy.legacy.utils.ivyMaxTime
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import java.time.Instant

@Suppress("DataClassFunctions")
@Immutable
data class FromToTimeRange(
    val from: Instant?,
    val to: Instant?,
) {
    fun from(): Instant =
        from ?: ivyMinTime()

    fun to(): Instant =
        to ?: ivyMaxTime()

    fun upcomingFrom(
        timeProvider: TimeProvider
    ): Instant {
        val startOfDayNowUTC = timeProvider.utcNow()
        return if (includes(startOfDayNowUTC)) startOfDayNowUTC else from()
    }

    fun overdueTo(
        timeProvider: TimeProvider
    ): Instant {
        val startOfDayNowUTC = timeProvider.utcNow()
        return if (includes(startOfDayNowUTC)) startOfDayNowUTC else to()
    }

    fun includes(dateTime: Instant): Boolean =
        dateTime.isAfter(from()) && dateTime.isBefore(to())

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

fun Iterable<com.ivy.data.model.Transaction>.filterUpcoming(
    timeProvider: TimeProvider,
): List<com.ivy.data.model.Transaction> {
    val todayStartOfDayUTC = timeProvider.localDateNow().atStartOfDay(timeProvider.getZoneId()).toInstant()

    return filter {
        // make sure that it's in the future
        !it.settled && it.time.isAfter(todayStartOfDayUTC)
    }
}

fun Iterable<com.ivy.data.model.Transaction>.filterOverdue(
    timeProvider: TimeProvider,
): List<com.ivy.data.model.Transaction> {
    val todayStartOfDayUTC = timeProvider.localDateNow().atStartOfDay(timeProvider.getZoneId()).toInstant()

    return filter {
        // make sure that it's in the past
        !it.settled && it.time.isBefore(todayStartOfDayUTC)
    }
}

fun FromToTimeRange.toCloseTimeRange(): ClosedTimeRange {
    return ClosedTimeRange(
        from = from ?: ivyMinTime(),
        to = to ?: ivyMaxTime()
    )
}

fun FromToTimeRange.toUTCCloseTimeRange(): ClosedTimeRange {
    return ClosedTimeRange(
        from = from ?: ivyMinTime(),
        to = to ?: ivyMaxTime()
    )
}
