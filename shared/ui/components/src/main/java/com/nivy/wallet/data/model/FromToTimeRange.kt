package com.nivy.wallet.data.model

import androidx.compose.runtime.Immutable
import com.nivy.base.time.TimeProvider
import com.nivy.wallet.utils.nivyMinTime
import com.nivy.wallet.utils.nivyMaxTime
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
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

// Due today counts as upcoming, earlier days as overdue.
private fun startOfToday(timeProvider: TimeProvider): Instant =
    timeProvider.localDateNow().atStartOfDay(timeProvider.getZoneId()).toInstant()

fun Iterable<com.nivy.data.model.Transaction>.filterUpcoming(
    timeProvider: TimeProvider,
): List<com.nivy.data.model.Transaction> {
    val startOfToday = startOfToday(timeProvider)
    return filter { !it.settled && !it.time.isBefore(startOfToday) }
}

fun Iterable<com.nivy.data.model.Transaction>.filterOverdue(
    timeProvider: TimeProvider,
): List<com.nivy.data.model.Transaction> {
    val startOfToday = startOfToday(timeProvider)
    return filter { !it.settled && it.time.isBefore(startOfToday) }
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
