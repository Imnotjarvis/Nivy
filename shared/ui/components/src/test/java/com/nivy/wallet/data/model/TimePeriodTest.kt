package com.nivy.wallet.data.model

import com.nivy.base.time.TimeProvider
import com.nivy.base.time.impl.TestTimeConverter
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TimePeriodTest {
    private val converter = TestTimeConverter()

    private fun timeProvider(today: LocalDate) = mockk<TimeProvider> {
        every { localDateNow() } returns today
        every { utcNow() } returns today.atTime(12, 0).toInstant(ZoneOffset.UTC)
        every { getZoneId() } returns ZoneOffset.UTC
    }

    @Test
    fun `with months starting on the 31st, the last day of April is in the current period`() {
        val today = LocalDate.of(2026, 4, 30)
        val time = timeProvider(today)

        val range = TimePeriod.currentMonth(startDayOfMonth = 31, timeProvider = time)
            .toRange(startDateOfMonth = 31, timeConverter = converter, timeProvider = time)

        range.includes(today.atTime(12, 0).toInstant(ZoneOffset.UTC)) shouldBe true
    }

    @Test
    fun `a range includes its first and last moments`() {
        val from = LocalDate.of(2026, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val to = LocalDate.of(2026, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)
        val range = FromToTimeRange(from = from, to = to)

        range.includes(from) shouldBe true
        range.includes(to) shouldBe true
    }

    @Test
    fun `upcoming starts at the start of today, so what is due earlier today shows`() {
        val today = LocalDate.of(2026, 10, 15)
        val range = FromToTimeRange(
            from = LocalDate.of(2026, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            to = LocalDate.of(2026, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
        )

        range.upcomingFrom(timeProvider(today)) shouldBe today.atStartOfDay().toInstant(ZoneOffset.UTC)
    }
}
