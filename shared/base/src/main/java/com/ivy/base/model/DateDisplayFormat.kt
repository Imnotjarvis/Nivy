package com.ivy.base.model

import androidx.annotation.Keep

/**
 * How the app shows dates, chosen in the settings. The formats with a month name leave the
 * year out for the current year; the numeric ones always show it.
 */
@Keep
enum class DateDisplayFormat(
    private val pattern: String,
    private val currentYearPattern: String,
) {
    DAY_MONTH_YEAR("d MMM yyyy", "d MMM"),
    MONTH_DAY_YEAR("MMM d, yyyy", "MMM d"),
    NUMERIC_DAY_MONTH_YEAR("dd/MM/yyyy", "dd/MM/yyyy"),
    ISO("yyyy-MM-dd", "yyyy-MM-dd");

    /** The [java.time.format.DateTimeFormatter] pattern of a date in the current year or not. */
    fun pattern(currentYear: Boolean): String = if (currentYear) currentYearPattern else pattern

    companion object {
        // The month-first style the app used before the setting existed.
        val Default = MONTH_DAY_YEAR
    }
}
