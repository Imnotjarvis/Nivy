package com.nivy.ui.time.impl

import com.nivy.base.resource.ResourceProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.ui.R
import com.nivy.ui.time.DateFormatState
import com.nivy.ui.time.DevicePreferences
import com.nivy.ui.time.TimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class NivyTimeFormatter @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val timeProvider: TimeProvider,
    private val converter: TimeConverter,
    private val devicePreferences: DevicePreferences,
    private val dateFormatState: DateFormatState,
) : TimeFormatter {

    override fun LocalDateTime.format(style: TimeFormatter.Style): String {
        val dateTime = this
        val today = timeProvider.localDateNow()
        val relativeDay = if (style.includeWeekDay) {
            this.getRelativeDay(today)
        } else {
            null
        }
        val pattern = buildString {
            if (relativeDay == null && style.includeWeekDay) {
                append("EEE, ")
            }
            append(dateFormatState.format.pattern(currentYear = dateTime.year == today.year))
            if (style is TimeFormatter.Style.DateAndTime) {
                append(" ")
                append(localeTimeFormat())
            }
        }
        val formatted = dateTime.format(
            // The app is in English, month and day names included.
            DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
        )
        val prefix = when (relativeDay) {
            RelativeDay.Yesterday -> resourceProvider.getString(R.string.yesterday)
            RelativeDay.Today -> resourceProvider.getString(R.string.today)
            RelativeDay.Tomorrow -> resourceProvider.getString(R.string.tomorrow)
            else -> null
        }

        return if (prefix != null) {
            "$prefix, $formatted"
        } else {
            formatted
        }
    }

    override fun LocalTime.format(): String = this.format(
        DateTimeFormatter.ofPattern(localeTimeFormat(), Locale.ENGLISH)
    )

    private fun localeTimeFormat(): String = if (devicePreferences.is24HourFormat()) {
        "HH:mm"
    } else {
        "h:mm a"
    }

    override fun Instant.formatLocal(style: TimeFormatter.Style): String {
        val instant = this
        val localDateTime = with(converter) { instant.toLocalDateTime() }
        return localDateTime.format(style)
    }

    private fun LocalDateTime.getRelativeDay(today: LocalDate): RelativeDay? {
        val date = this.toLocalDate()
        return when (date) {
            today -> RelativeDay.Today
            today.plusDays(1) -> RelativeDay.Tomorrow
            today.minusDays(1) -> RelativeDay.Yesterday
            else -> null
        }
    }

    enum class RelativeDay {
        Yesterday, Today, Tomorrow,
    }
}
