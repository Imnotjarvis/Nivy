package com.nivy.base.time.impl

import com.nivy.base.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** A clock stopped at [now], in [zone]: for tests, as [TestTimeConverter] is. */
class FixedTimeProvider(
    private val now: LocalDateTime,
    private val zone: ZoneId = ZoneId.of("UTC"),
) : TimeProvider {
    override fun getZoneId(): ZoneId = zone
    override fun utcNow(): Instant = now.atZone(zone).toInstant()
    override fun localNow(): LocalDateTime = now
    override fun localDateNow(): LocalDate = now.toLocalDate()
    override fun localTimeNow(): LocalTime = now.toLocalTime()
}
