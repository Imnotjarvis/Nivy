package com.nivy.data.repository

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.YearMonth

class ConsumerPricesTest {
    private val prices = ConsumerPrices(
        mapOf(
            YearMonth.of(2026, 1) to 100.0,
            YearMonth.of(2026, 3) to 105.0,
            YearMonth.of(2026, 4) to 110.0,
        )
    )

    @Test
    fun `an amount of a past month is brought to the newest month's money`() {
        prices.latest shouldBe YearMonth.of(2026, 4)
        prices.toLatest(YearMonth.of(2026, 1)) shouldBe 1.1
        prices.toLatest(YearMonth.of(2026, 3)) shouldBe 110.0 / 105.0
    }

    @Test
    fun `the newest month, and those after it without an index yet, are taken as they are`() {
        prices.toLatest(YearMonth.of(2026, 4)) shouldBe 1.0
        prices.toLatest(YearMonth.of(2026, 6)) shouldBe 1.0
    }

    @Test
    fun `a month without an index has no factor`() {
        prices.toLatest(YearMonth.of(2025, 12)) shouldBe null
        prices.toLatest(YearMonth.of(2026, 2)) shouldBe null
        ConsumerPrices(emptyMap()).toLatest(YearMonth.of(2026, 1)) shouldBe null
    }

    @Test
    fun `the built-in index runs month by month from 2016, and averages 100 over 2025, its base`() {
        val months = BuiltInNic.keys.sorted()
        months.first() shouldBe YearMonth.of(2016, 1)
        months.zipWithNext().forEach { (month, next) -> next shouldBe month.plusMonths(1) }
        BuiltInNic.filterKeys { it.year == 2025 }.values.average() shouldBe (100.0 plusOrMinus 0.01)
    }
}
