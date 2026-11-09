package com.nivy.home

import com.nivy.base.model.TransactionType
import com.nivy.home.usecase.Glance
import com.nivy.home.usecase.NextPayment
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.math.BigDecimal

class GlanceFactsTest {
    @Test
    fun `the glance shows what's overdue first, then today's spending, then the next payment`() {
        // given
        val glance = Glance(overdue = 1, spentToday = BigDecimal("12.40"), baseCurrency = "EUR", nextPayment = Rent)

        // when
        val facts = glance.facts()

        // then
        facts shouldBe listOf(GlanceFact.Overdue, GlanceFact.SpentToday, GlanceFact.NextPayment)
    }

    @Test
    fun `with nothing overdue or due, the glance shows today's spending, even when nothing was spent`() {
        // given
        val glance = Glance(overdue = 0, spentToday = BigDecimal.ZERO, baseCurrency = "EUR", nextPayment = null)

        // when
        val facts = glance.facts()

        // then
        facts shouldBe listOf(GlanceFact.SpentToday)
    }

    private companion object {
        val Rent = NextPayment(name = "Rent", type = TransactionType.EXPENSE, amount = 850.0, currency = "EUR", daysLeft = 3)
    }
}
