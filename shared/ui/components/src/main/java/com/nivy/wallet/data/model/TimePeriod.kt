package com.nivy.wallet.data.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Immutable
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.wallet.utils.atEndOfDay
import com.nivy.wallet.utils.endOfMonth
import com.nivy.wallet.utils.startOfMonth
import com.nivy.wallet.utils.withDayOfMonthSafe
import com.nivy.ui.time.TimeFormatter
import java.time.Instant
import java.time.LocalDate

private const val MonthNameAbbreviationLength = 3

@Immutable
data class TimePeriod(
    val month: Month? = null,
    val year: Int? = null,
    val fromToRange: FromToTimeRange? = null,
    val lastNRange: LastNTimeRange? = null,
) {
    companion object {
        /**
         * Examples:
         * 1. startDateOfMonth = 1, today = Nov. 10
         * return Nov. 1 - Nov. 30
         *
         * 2. startDateOfMonth = 10, today = Nov. 9
         * return Oct. 10 - Nov. 9
         *
         * 3. startDateOfMonth = 10, today = Nov. 10
         * return Nov. 10 - Dec. 9
         */
        fun currentMonth(startDayOfMonth: Int, timeProvider: TimeProvider): TimePeriod {
            val today = timeProvider.localDateNow()
            val dayToday = today.dayOfMonth

            // Examples month = Nov. startDate = 7; Period = from Nov (7) till Dec (6)
            // => new period starts if today => startDayOfMonth
            // A start day past the month's end (31 in April) is its last day, as toRange counts it.
            val newPeriodStarted = dayToday >= minOf(startDayOfMonth, today.lengthOfMonth())

            val periodDate = if (newPeriodStarted) {
                // new monthly period has already started then observe it => current month
                today
            } else {
                // new monthly period hasn't yet started then observe the ongoing one => previous month
                today.minusMonths(1)
            }

            return TimePeriod(
                month = Month.fromMonthValue(
                    periodDate.monthValue
                ),
                year = periodDate.year
            )
        }
    }

    fun isValid(): Boolean =
        month != null || fromToRange != null || lastNRange != null

    fun toRange(
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
        timeProvider: TimeProvider,
    ): FromToTimeRange = with(timeConverter) {
        when {
            month != null -> {
                val date = month.firstDay(year ?: timeProvider.localDateNow().year)
                val (from, to) = if (startDateOfMonth != 1) {
                    customStartDayOfMonthPeriodRange(
                        date = date,
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                    )
                } else {
                    Pair(startOfMonth(date, timeConverter), endOfMonth(date, timeConverter))
                }

                FromToTimeRange(
                    from = from,
                    to = to
                )
            }

            fromToRange != null -> {
                fromToRange
            }

            lastNRange != null -> {
                FromToTimeRange(
                    from = lastNRange.fromDate(timeProvider),
                    to = timeProvider.utcNow()
                )
            }

            else -> {
                val date = timeProvider.localDateNow()
                FromToTimeRange(
                    from = startOfMonth(date, timeConverter),
                    to = endOfMonth(date, timeConverter)
                )
            }
        }
    }

    private fun customStartDayOfMonthPeriodRange(
        date: LocalDate,
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
    ): Pair<Instant, Instant> = with(timeConverter) {
        val from = date
            .withDayOfMonthSafe(startDateOfMonth)
            .atStartOfDay()
            .toUTC()

        val to = date
            // startDayOfMonth != 1 just shift N day the month forward so to should +1 month
            .plusMonths(1)
            .withDayOfMonthSafe(startDateOfMonth)
            // e.g. Correct: 14.10-13.11 (Incorrect: 14.10-14.11)
            .minusDays(1)
            .atEndOfDay()
            .toUTC()

        from to to
    }

    @Composable
    @ReadOnlyComposable
    fun toDisplayShort(
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
        timeProvider: TimeProvider,
        timeFormatter: TimeFormatter,
        // "Nov" rather than "November" for a month of this year, where room is short.
        shortMonth: Boolean = false,
    ): String = with(timeFormatter) {
        when {
            month != null -> {
                if (startDateOfMonth == 1) {
                    displayMonthStartingOn1st(month = month, timeProvider, shortMonth)
                } else {
                    val range = toRange(
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider,
                    )
                    val style = TimeFormatter.Style.DateOnly(includeWeekDay = false)
                    "${range.from?.formatLocal(style)} - ${range.to?.formatLocal(style)}"
                }
            }

            fromToRange != null -> {
                fromToRange.toDisplay(timeFormatter)
            }

            lastNRange != null -> {
                "Last ${lastNRange.forDisplay()}"
            }

            else -> "Custom"
        }
    }

    @Composable
    @ReadOnlyComposable
    fun toDisplayLong(
        startDateOfMonth: Int,
        timeProvider: TimeProvider,
        timeConverter: TimeConverter,
        timeFormatter: TimeFormatter
    ): String {
        return when {
            month != null -> {
                if (startDateOfMonth == 1) {
                    displayMonthStartingOn1st(month = month, timeProvider)
                } else {
                    toRange(
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider
                    ).toDisplay(timeFormatter)
                }
            }

            fromToRange != null -> {
                fromToRange.toDisplay(timeFormatter)
            }

            lastNRange != null -> {
                "the last ${lastNRange.forDisplay()}"
            }

            else -> {
                toRange(
                    startDateOfMonth = startDateOfMonth,
                    timeConverter = timeConverter,
                    timeProvider = timeProvider
                ).toDisplay(timeFormatter)
            }
        }
    }

    @Composable
    @ReadOnlyComposable
    private fun displayMonthStartingOn1st(
        month: Month,
        timeProvider: TimeProvider,
        shortMonth: Boolean = false,
    ): String {
        val year = year
        return if (year != null && timeProvider.localNow().year != year) {
            // not this year
            "${month.displayName().take(MonthNameAbbreviationLength)}, $year"
        } else {
            // this year
            month.displayName().let { if (shortMonth) it.take(MonthNameAbbreviationLength) else it }
        }
    }
}
