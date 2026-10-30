package com.ivy.legacy

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.data.model.IntervalType
import com.ivy.ui.R
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Composable
fun IntervalType.forDisplay(intervalN: Int): String {
    val plural = intervalN > 1 || intervalN == 0
    return when (this) {
        IntervalType.DAY -> if (plural) stringResource(R.string.days) else stringResource(R.string.day)
        IntervalType.WEEK -> if (plural) stringResource(R.string.weeks) else stringResource(R.string.week)
        IntervalType.MONTH -> if (plural) stringResource(R.string.months) else stringResource(R.string.month)
        IntervalType.YEAR -> if (plural) stringResource(R.string.years) else stringResource(R.string.year)
    }
}

@Suppress("MagicNumber")
fun IntervalType.incrementDate(date: Instant, intervalN: Long): Instant {
    return when (this) {
        IntervalType.DAY -> date.plus(intervalN, ChronoUnit.DAYS)
        IntervalType.WEEK -> date.plus(intervalN * 7, ChronoUnit.DAYS)
        IntervalType.MONTH -> date.atZone(ZoneOffset.UTC).plusMonths(intervalN).toInstant()
        IntervalType.YEAR -> date.atZone(ZoneOffset.UTC).plusYears(intervalN).toInstant()
    }
}
