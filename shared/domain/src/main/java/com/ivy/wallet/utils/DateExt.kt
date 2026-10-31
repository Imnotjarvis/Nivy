package com.ivy.wallet.utils

import com.ivy.base.time.INSTANT_MAX_SAFE
import com.ivy.base.time.INSTANT_MIN_SAFE
import com.ivy.base.time.TimeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// A timestamp for file names, like 20261030-0915.
fun LocalDateTime.getISOFormattedDateTime(): String = this.format("yyyyMMdd-HHmm")

fun LocalDateTime.format(
    pattern: String
): String {
    return this.format(
        DateTimeFormatter.ofPattern(pattern)
    )
}

fun LocalDate.formatLocal(
    pattern: String = "dd MMM yyyy",
    zone: ZoneId = ZoneOffset.systemDefault()
): String {
    return this.format(
        DateTimeFormatter
            .ofPattern(pattern)
            .withLocale(Locale.getDefault())
            .withZone(zone) // this is if you want to display the Zone in the pattern
    )
}

fun startOfMonth(date: LocalDate, timeConverter: TimeConverter): Instant {
    val startOfMonthLocal = date.withDayOfMonth(1).atStartOfDay()
    return with(timeConverter) { startOfMonthLocal.toUTC() }
}

fun endOfMonth(date: LocalDate, timeConverter: TimeConverter): Instant {
    val endOfMonthLocal = date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX)
    return with(timeConverter) { endOfMonthLocal.toUTC() }
}

fun LocalDate.atEndOfDay(): LocalDateTime =
    this.atTime(LocalTime.MAX.truncatedTo(ChronoUnit.SECONDS))

fun ivyMinTime(): Instant = INSTANT_MIN_SAFE

fun ivyMaxTime(): Instant = INSTANT_MAX_SAFE

fun LocalDate.withDayOfMonthSafe(targetDayOfMonth: Int): LocalDate {
    val maxDayOfMonth = this.lengthOfMonth()
    return this.withDayOfMonth(
        if (targetDayOfMonth > maxDayOfMonth) maxDayOfMonth else targetDayOfMonth
    )
}
