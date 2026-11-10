package com.nivy.wallet.domain.pure.forecast

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

class ForecastTest {
    @Test
    fun `a band is the 8th, 50th and 92nd percentiles`() {
        // Between two values, as far along as the share falls.
        listOf(DoubleArray(11) { it.toDouble() }, doubleArrayOf(0.0, 10.0)).forEach { values ->
            val band = band(values)
            band.low shouldBe (0.8 plusOrMinus 1e-9)
            band.likely shouldBe (5.0 plusOrMinus 1e-9)
            band.high shouldBe (9.2 plusOrMinus 1e-9)
        }
    }

    @Test
    fun `each day ahead spends as a past day on its weekday did`() {
        // given four weeks in which only Mondays spend, 20 each
        val history = days(LocalDate.of(2026, 10, 12), 28).associateWith { day ->
            if (day.dayOfWeek == DayOfWeek.MONDAY) 20.0 else 0.0
        }

        // when the week from Monday the 9th runs
        val runs = simulateDays(history, days(LocalDate.of(2026, 11, 9), 7), Random(1))

        // then every run spends the 20 on Monday, and nothing after
        runs.size shouldBe ForecastRuns
        runs.forEach { it.toList() shouldBe List(7) { 20.0 } }
    }

    @Test
    fun `the last weeks count more than older ones`() {
        // given six weeks at 10 a day, then six at 30
        val history = days(LocalDate.of(2026, 8, 17), 84).withIndex()
            .associate { (index, day) -> day to if (index < 42) 10.0 else 30.0 }

        // when
        val firstDays = simulateDays(history, listOf(LocalDate.of(2026, 11, 9)), Random(2)).map { it[0] }

        // then the last six weeks weigh twice the six before: 30 two times in 3
        firstDays.average() shouldBe (70.0 / 3 plusOrMinus 1.0)
    }

    @Test
    fun `a simulation runs the same from the same seed`() {
        val history = days(LocalDate.of(2026, 10, 1), 39).associateWith { it.dayOfMonth.toDouble() }
        val ahead = days(LocalDate.of(2026, 11, 9), 21)

        simulateDays(history, ahead, Random(3)).map { it.toList() } shouldBe
            simulateDays(history, ahead, Random(3)).map { it.toList() }
    }

    @Test
    fun `months that always move alike draw a line, with no band`() {
        // when
        val bands = bandsByStep(simulateMonths(1_000.0, listOf(50.0, 50.0, 50.0), months = 3, Random(4)))

        // then
        bands shouldBe listOf(
            Band(1_050.0, 1_050.0, 1_050.0),
            Band(1_100.0, 1_100.0, 1_100.0),
            Band(1_150.0, 1_150.0, 1_150.0),
        )
    }

    @Test
    fun `a target is reached early, likely and late, as the runs reach it`() {
        // given ten runs that reach 100 in steps 1, 2, 2, 3, 3, 3, 3, 4 and 5, and one that never does
        val steps = listOf(1, 2, 2, 3, 3, 3, 3, 4, 5, null)
        val runs = steps.map { step ->
            DoubleArray(6) { index -> if (step != null && index + 1 >= step) 100.0 else 0.0 }
        }

        // then
        reach(runs, 100.0) shouldBe Reach(early = 1, likely = 3, late = 5)
        // With two in ten that never do, how late isn't known.
        reach(runs.dropLast(2) + runs.last() + runs.last(), 100.0) shouldBe Reach(early = 1, likely = 3, late = null)
        // With half of them that never do, it isn't likely reached.
        reach(runs.take(4) + List(6) { runs.last() }, 100.0).shouldBeNull()
    }

    @Test
    fun `a new level from a month on is a step`() {
        // when
        val shift = shift(listOf(100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 200.0, 200.0, 200.0, 200.0, 200.0), 10.0)

        // then
        shift shouldBe Shift.Step(at = 6, before = 100.0, after = 200.0)
    }

    @Test
    fun `a steady move is a drift`() {
        // when
        val shift = shift((0..11).map { 100.0 + 10.0 * it }, 10.0)

        // then
        shift.shouldBeInstanceOf<Shift.Drift>()
        shift.perMonth shouldBe (10.0 plusOrMinus 1e-9)
        shift.before shouldBe (100.0 plusOrMinus 1e-9)
        shift.after shouldBe (210.0 plusOrMinus 1e-9)
    }

    @Test
    fun `noise, a one-off and small moves aren't shifts`() {
        // A month of a big purchase among months alike.
        shift(listOf(100.0, 120.0, 90.0, 110.0, 100.0, 95.0, 105.0, 100.0, 1_200.0, 100.0, 98.0, 102.0), 10.0)
            .shouldBeNull()
        // Moves by less than the least, or less than a fifth.
        shift(listOf(100.0, 100.0, 100.0, 105.0, 105.0, 105.0), 10.0).shouldBeNull()
        shift(listOf(100.0, 100.0, 100.0, 115.0, 115.0, 115.0), 10.0).shouldBeNull()
        // Fewer than 6 months.
        shift(listOf(100.0, 100.0, 200.0, 200.0, 200.0), 10.0).shouldBeNull()
    }

    @Test
    fun `8 times in 10, the spending of the weeks ahead lands in its band`() {
        // given days that spend nothing 6 times in 10, and 5 to 60 otherwise
        val random = Random(5)
        fun day() = if (random.nextDouble() < 0.6) 0.0 else 5.0 + random.nextDouble() * 55.0

        // when the 3 weeks after 13 weeks of history are forecast, and then happen
        val landed = (1..Trials).count { trial ->
            val history = days(LocalDate.of(2026, 8, 10), 91).associateWith { day() }
            val ahead = days(LocalDate.of(2026, 11, 9), 21)
            val total = band(simulateDays(history, ahead, Random(trial)).map { it.last() }.toDoubleArray())
            ahead.sumOf { day() } in total.low..total.high
        }

        // then
        (landed.toDouble() / Trials).shouldBeBetween(0.75, 0.85, 0.0)
    }

    @Test
    fun `8 times in 10, the net worth 3 months on lands in its band`() {
        // given months that save 300 to 1,200, and lose 1,200 more one time in 7 or so
        val random = Random(6)
        fun month() = 300.0 + random.nextDouble() * 900.0 - if (random.nextDouble() < 0.15) 1_200.0 else 0.0

        // when 3 months after 12 are forecast, and then happen
        val landed = (1..Trials).count { trial ->
            val band = bandsByStep(simulateMonths(0.0, List(12) { month() }, months = 3, Random(trial))).last()
            (1..3).sumOf { month() } in band.low..band.high
        }

        // then
        (landed.toDouble() / Trials).shouldBeBetween(0.75, 0.85, 0.0)
    }

    private fun days(from: LocalDate, count: Int): List<LocalDate> = (0 until count).map { from.plusDays(it.toLong()) }

    private companion object {
        const val Trials = 400
    }
}
