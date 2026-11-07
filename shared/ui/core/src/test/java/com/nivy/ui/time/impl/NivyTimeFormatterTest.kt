package com.nivy.ui.time.impl

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.resource.TestResourceProvider
import com.nivy.base.time.TimeConverter
import com.nivy.base.time.TimeProvider
import com.nivy.ui.R
import com.nivy.base.model.DateDisplayFormat
import com.nivy.ui.time.DateFormatState
import com.nivy.ui.time.DevicePreferences
import com.nivy.ui.time.TimeFormatter.Style
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

@RunWith(TestParameterInjector::class)
class NivyTimeFormatterTest {

    private val timeProvider = mockk<TimeProvider>()
    private val converter = mockk<TimeConverter>()
    private val devicePreferences = mockk<DevicePreferences>()
    private val dateFormatState = DateFormatState()

    private lateinit var formatter: NivyTimeFormatter

    @Before
    fun setup() {
        formatter = NivyTimeFormatter(
            resourceProvider = TestResourceProvider().apply {
                putString(R.string.yesterday, "Yesterday")
                putString(R.string.today, "Today")
                putString(R.string.tomorrow, "Tomorrow")
            },
            timeProvider = timeProvider,
            converter = converter,
            devicePreferences = devicePreferences,
            dateFormatState = dateFormatState,
        )
    }

    enum class DateOnlyTestCases(
        val date: LocalDateTime,
        val style: Style.DateOnly,
        val today: LocalDate,
        val expectedFormatted: String
    ) {
        WITH_WEEK_DAY__TODAY(
            date = LocalDateTime.of(2021, 1, 1, 0, 0),
            style = Style.DateOnly(includeWeekDay = true),
            today = LocalDate.of(2021, 1, 1),
            expectedFormatted = "Today, Jan 1"
        ),
        WITH_WEEK_DAY__YESTERDAY(
            date = LocalDateTime.of(2021, 2, 23, 0, 0),
            style = Style.DateOnly(includeWeekDay = true),
            today = LocalDate.of(2021, 2, 24),
            expectedFormatted = "Yesterday, Feb 23"
        ),
        WITH_WEEK_DAY__TOMORROW(
            date = LocalDateTime.of(2021, 9, 8, 0, 0),
            style = Style.DateOnly(includeWeekDay = true),
            today = LocalDate.of(2021, 9, 7),
            expectedFormatted = "Tomorrow, Sep 8"
        ),
        WITH_WEEK_DAY__NON_RELATIVE(
            date = LocalDateTime.of(2024, 8, 29, 0, 0),
            style = Style.DateOnly(includeWeekDay = true),
            today = LocalDate.of(2024, 8, 7),
            expectedFormatted = "Thu, Aug 29"
        ),
        WITH_WEEK_DAY__DIFFERENT_YEAR(
            date = LocalDateTime.of(2024, 8, 29, 0, 0),
            style = Style.DateOnly(includeWeekDay = true),
            today = LocalDate.of(2023, 8, 7),
            expectedFormatted = "Thu, Aug 29, 2024"
        ),
        NO_WEEK_DAY__TODAY(
            date = LocalDateTime.of(2021, 1, 1, 0, 0),
            style = Style.DateOnly(includeWeekDay = false),
            today = LocalDate.of(2021, 1, 1),
            expectedFormatted = "Jan 1"
        ),
        NO_WEEK_DAY__YESTERDAY(
            date = LocalDateTime.of(2021, 2, 23, 0, 0),
            style = Style.DateOnly(includeWeekDay = false),
            today = LocalDate.of(2021, 2, 24),
            expectedFormatted = "Feb 23"
        ),
        NO_WEEK_DAY__TOMORROW(
            date = LocalDateTime.of(2021, 9, 8, 0, 0),
            style = Style.DateOnly(includeWeekDay = false),
            today = LocalDate.of(2021, 9, 7),
            expectedFormatted = "Sep 8"
        ),
        NO_WEEK_DAY__NON_RELATIVE(
            date = LocalDateTime.of(2024, 8, 29, 0, 0),
            style = Style.DateOnly(includeWeekDay = false),
            today = LocalDate.of(2024, 8, 7),
            expectedFormatted = "Aug 29"
        ),
        NO_WEEK_DAY__DIFFERENT_YEAR(
            date = LocalDateTime.of(2024, 8, 29, 0, 0),
            style = Style.DateOnly(includeWeekDay = false),
            today = LocalDate.of(2023, 8, 7),
            expectedFormatted = "Aug 29, 2024"
        ),
    }

    @Test
    fun `validate date-only formatting`(
        @TestParameter testCase: DateOnlyTestCases
    ) {
        // Given
        every { timeProvider.localDateNow() } returns testCase.today
        val date = testCase.date

        // When
        val formatted = with(formatter) { date.format(testCase.style) }

        // Then
        formatted shouldBe testCase.expectedFormatted
    }

    enum class DateAndTimeTestCases(
        val date: LocalDateTime,
        val style: Style.DateAndTime,
        val today: LocalDate,
        val is24HourFormat: Boolean,
        val expectedFormatted: String
    ) {
        TODAY_24H_FORMAT(
            date = LocalDateTime.of(2021, 1, 1, 12, 30),
            style = Style.DateAndTime(includeWeekDay = true),
            today = LocalDate.of(2021, 1, 1),
            is24HourFormat = true,
            expectedFormatted = "Today, Jan 1 12:30"
        ),
        TODAY_AM_PM_FORMAT(
            date = LocalDateTime.of(2021, 1, 1, 12, 30),
            style = Style.DateAndTime(includeWeekDay = true),
            today = LocalDate.of(2021, 1, 1),
            is24HourFormat = false,
            expectedFormatted = "Today, Jan 1 12:30 PM"
        ),
        NEW_YEAR_EVE_AM_PM(
            date = LocalDateTime.of(2021, 12, 31, 23, 59),
            style = Style.DateAndTime(includeWeekDay = true),
            today = LocalDate.of(2022, 1, 1),
            is24HourFormat = false,
            expectedFormatted = "Yesterday, Dec 31, 2021 11:59 PM"
        ),
        NEW_YEAR_EVE_24H(
            date = LocalDateTime.of(2021, 12, 31, 23, 59),
            style = Style.DateAndTime(includeWeekDay = true),
            today = LocalDate.of(2022, 1, 1),
            is24HourFormat = true,
            expectedFormatted = "Yesterday, Dec 31, 2021 23:59"
        ),
        RANDOM_DAY_7AM(
            date = LocalDateTime.of(2023, 5, 4, 7, 20),
            style = Style.DateAndTime(includeWeekDay = false),
            today = LocalDate.of(2023, 1, 1),
            is24HourFormat = false,
            expectedFormatted = "May 4 7:20 AM"
        ),
    }

    @Test
    fun `validate date and time formatting`(
        @TestParameter testCase: DateAndTimeTestCases
    ) {
        // Given
        every { timeProvider.localDateNow() } returns testCase.today
        every { devicePreferences.is24HourFormat() } returns testCase.is24HourFormat
        val date = testCase.date

        // When
        val formatted = with(formatter) { date.format(testCase.style) }

        // Then
        formatted shouldBe testCase.expectedFormatted
    }

    enum class TimeFormattingTestCase(
        val time: LocalTime,
        val is24HourFormat: Boolean,
        val expectedFormatted: String
    ) {
        H24_7_30(
            time = LocalTime.of(7, 30),
            is24HourFormat = true,
            expectedFormatted = "07:30"
        ),
        H24_19_30(
            time = LocalTime.of(19, 30),
            is24HourFormat = true,
            expectedFormatted = "19:30"
        ),
        AM_7_30(
            time = LocalTime.of(7, 30),
            is24HourFormat = false,
            expectedFormatted = "7:30 AM"
        ),
        PM_7_30(
            time = LocalTime.of(19, 30),
            is24HourFormat = false,
            expectedFormatted = "7:30 PM"
        ),
    }

    @Test
    fun `validate time formatting`(
        @TestParameter testCase: TimeFormattingTestCase
    ) {
        // Given
        every { devicePreferences.is24HourFormat() } returns testCase.is24HourFormat
        val time = testCase.time

        // When
        val formatted = with(formatter) { time.format() }

        // Then
        formatted shouldBe testCase.expectedFormatted
    }

    @Test
    fun `validate Instant - formatLocal (date-only)`(
        @TestParameter testCase: DateOnlyTestCases
    ) {
        // Given
        val instant = Instant.EPOCH
        with(converter) {
            every { instant.toLocalDateTime() } returns testCase.date
        }
        every { timeProvider.localDateNow() } returns testCase.today

        // When
        val formatted = with(formatter) { instant.formatLocal(testCase.style) }

        // Then
        formatted shouldBe testCase.expectedFormatted
    }

    enum class DateFormatTestCase(
        val format: DateDisplayFormat,
        val today: LocalDate,
        val expectedFormatted: String,
    ) {
        DAY_MONTH_YEAR__CURRENT_YEAR(DateDisplayFormat.DAY_MONTH_YEAR, LocalDate.of(2024, 1, 1), "Thu, 29 Aug"),
        DAY_MONTH_YEAR__OTHER_YEAR(DateDisplayFormat.DAY_MONTH_YEAR, LocalDate.of(2025, 1, 1), "Thu, 29 Aug 2024"),
        MONTH_DAY_YEAR__CURRENT_YEAR(DateDisplayFormat.MONTH_DAY_YEAR, LocalDate.of(2024, 1, 1), "Thu, Aug 29"),
        MONTH_DAY_YEAR__OTHER_YEAR(DateDisplayFormat.MONTH_DAY_YEAR, LocalDate.of(2025, 1, 1), "Thu, Aug 29, 2024"),
        NUMERIC__CURRENT_YEAR(DateDisplayFormat.NUMERIC_DAY_MONTH_YEAR, LocalDate.of(2024, 1, 1), "Thu, 29/08/2024"),
        NUMERIC__OTHER_YEAR(DateDisplayFormat.NUMERIC_DAY_MONTH_YEAR, LocalDate.of(2025, 1, 1), "Thu, 29/08/2024"),
        ISO__CURRENT_YEAR(DateDisplayFormat.ISO, LocalDate.of(2024, 1, 1), "Thu, 2024-08-29"),
        ISO__OTHER_YEAR(DateDisplayFormat.ISO, LocalDate.of(2025, 1, 1), "Thu, 2024-08-29"),
    }

    @Test
    fun `dates follow the chosen format`(
        @TestParameter testCase: DateFormatTestCase
    ) {
        // Given
        every { timeProvider.localDateNow() } returns testCase.today
        dateFormatState.format = testCase.format

        // When
        val formatted = with(formatter) {
            LocalDateTime.of(2024, 8, 29, 14, 30).format(Style.DateOnly(includeWeekDay = true))
        }

        // Then
        formatted shouldBe testCase.expectedFormatted
    }

    @Test
    fun `month and day names are in English whatever the phone's language`() {
        // Given
        val phoneLocale = Locale.getDefault()
        Locale.setDefault(Locale.ITALIAN)
        every { timeProvider.localDateNow() } returns LocalDate.of(2024, 1, 1)
        every { devicePreferences.is24HourFormat() } returns false
        dateFormatState.format = DateDisplayFormat.DAY_MONTH_YEAR

        try {
            // When
            val formatted = with(formatter) {
                LocalDateTime.of(2024, 8, 29, 14, 30).format(Style.DateAndTime(includeWeekDay = true))
            }

            // Then
            formatted shouldBe "Thu, 29 Aug 2:30 PM"
        } finally {
            Locale.setDefault(phoneLocale)
        }
    }
}
