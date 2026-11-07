package com.nivy.ui.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

interface TimeFormatter {
    fun LocalDateTime.format(style: Style): String
    fun LocalTime.format(): String
    fun Instant.formatLocal(style: Style): String

    /**
     * The date follows the format chosen in the settings; the time follows the phone's 12/24h.
     * With the default format:
     * - Today, Aug 29
     * - Thu, Aug 29, 2025
     * - Yesterday, May 5 17:03
     * - Sep 15 22:21
     * - Oct 16 12:30 PM
     */
    sealed interface Style {
        val includeWeekDay: Boolean

        /**
         * @param includeWeekDay whether to include Yesterday/Today/Tomorrow/Mon/Tue/Wed/.../Sun
         */
        data class DateAndTime(
            override val includeWeekDay: Boolean,
        ) : Style

        /**
         * @param includeWeekDay whether to include Yesterday/Today/Tomorrow/Mon/Tue/Wed/.../Sun
         */
        data class DateOnly(
            override val includeWeekDay: Boolean,
        ) : Style
    }
}
