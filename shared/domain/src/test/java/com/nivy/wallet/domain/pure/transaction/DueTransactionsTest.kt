package com.nivy.wallet.domain.pure.transaction

import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Transaction
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DueTransactionsTest {

    @Test
    fun `what's due today or later is upcoming, earlier days are overdue, and paid ones are neither`() {
        // given
        val earlierToday = due("2026-11-15T08:00:00Z")
        val yesterday = due("2026-11-14T23:00:00Z")
        val tomorrow = due("2026-11-16T09:00:00Z")
        val paidYesterday = due("2026-11-14T10:00:00Z", settled = true)
        val transactions = listOf(earlierToday, yesterday, tomorrow, paidYesterday)

        // when
        val upcoming = transactions.filterUpcoming(noonOnNov15("UTC"))
        val overdue = transactions.filterOverdue(noonOnNov15("UTC"))

        // then
        upcoming shouldBe listOf(earlierToday, tomorrow)
        overdue shouldBe listOf(yesterday)
    }

    @Test
    fun `today starts at midnight where the phone is`() {
        // given: half past midnight of November 15 in Rome, still the 14th in UTC
        val justAfterMidnight = due("2026-11-14T23:30:00Z")

        // when
        val upcoming = listOf(justAfterMidnight).filterUpcoming(noonOnNov15("Europe/Rome"))

        // then
        upcoming shouldBe listOf(justAfterMidnight)
    }

    private fun due(time: String, settled: Boolean = false) = mockk<Transaction> {
        every { this@mockk.time } returns Instant.parse(time)
        every { this@mockk.settled } returns settled
    }

    private fun noonOnNov15(zone: String) = object : TimeProvider {
        private val now = LocalDateTime.of(2026, 11, 15, 12, 0)
        override fun getZoneId(): ZoneId = ZoneId.of(zone)
        override fun utcNow(): Instant = now.atZone(getZoneId()).toInstant()
        override fun localNow(): LocalDateTime = now
        override fun localDateNow(): LocalDate = now.toLocalDate()
        override fun localTimeNow(): LocalTime = now.toLocalTime()
    }
}
