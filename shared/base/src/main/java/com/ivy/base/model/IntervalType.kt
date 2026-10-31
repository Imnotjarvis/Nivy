package com.ivy.base.model

import androidx.annotation.Keep
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Keep
enum class IntervalType {
    DAY, WEEK, MONTH, YEAR
}

/** [date] moved forward by [intervalN] intervals. Months and years are counted in UTC. */
@Suppress("MagicNumber")
fun IntervalType.incrementDate(date: Instant, intervalN: Long): Instant {
    return when (this) {
        IntervalType.DAY -> date.plus(intervalN, ChronoUnit.DAYS)
        IntervalType.WEEK -> date.plus(intervalN * 7, ChronoUnit.DAYS)
        IntervalType.MONTH -> date.atZone(ZoneOffset.UTC).plusMonths(intervalN).toInstant()
        IntervalType.YEAR -> date.atZone(ZoneOffset.UTC).plusYears(intervalN).toInstant()
    }
}
