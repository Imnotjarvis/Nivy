package com.ivy.base.model

import androidx.annotation.Keep
import java.time.Instant
import java.time.ZoneId

@Keep
enum class IntervalType {
    DAY, WEEK, MONTH, YEAR
}

/**
 * [date] moved forward by [intervalN] intervals on [zone]'s calendar, so a monthly payment on
 * the 1st stays on the 1st and a daily one keeps its local time across DST changes. Counting in
 * UTC put rules created near midnight or far from UTC on the wrong day.
 */
fun IntervalType.incrementDate(date: Instant, intervalN: Long, zone: ZoneId): Instant {
    val local = date.atZone(zone)
    return when (this) {
        IntervalType.DAY -> local.plusDays(intervalN)
        IntervalType.WEEK -> local.plusWeeks(intervalN)
        IntervalType.MONTH -> local.plusMonths(intervalN)
        IntervalType.YEAR -> local.plusYears(intervalN)
    }.toInstant()
}
