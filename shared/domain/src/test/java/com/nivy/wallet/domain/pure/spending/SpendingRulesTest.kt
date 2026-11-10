package com.nivy.wallet.domain.pure.spending

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** The rules both stats screens count by. */
class SpendingRulesTest {
    @Test
    fun `the habit tiles count the purchases made by choice, not bills, subscriptions or loans`() {
        Flows.chosen() shouldBe listOf(Lunch, Shoes)
    }

    @Test
    fun `fixed is the bills and subscriptions, and a loan's repayment isn't one`() {
        Flows.fixed() shouldBe listOf(Rent)
    }

    @Test
    fun `a day with only a bill is a day without spending, and days before tracking aren't counted`() {
        val days = (1..5).map { november(it) }

        // Lunch on the 2nd and shoes on the 4th; the rent on the 1st and the loan on the 3rd don't count.
        noSpendDays(days, Flows) shouldBe 3
        noSpendDays(days.trackedSince(november(3)), Flows) shouldBe 2
        days.trackedSince(null) shouldBe days
    }

    private companion object {
        fun november(day: Int): LocalDate = LocalDate.of(2026, 11, day)

        fun flow(day: Int, amount: Double, recurring: Boolean = false, purchase: Boolean = true) = Flow(
            amount = amount,
            income = false,
            day = november(day),
            made = LocalDateTime.of(2026, 11, day, 12, 0),
            title = null,
            category = null,
            account = null,
            recurring = recurring,
            purchase = purchase,
        )

        val Rent = flow(1, 550.0, recurring = true)
        val Lunch = flow(2, 12.0)
        val LoanRepayment = flow(3, 200.0, recurring = true, purchase = false)
        val Shoes = flow(4, 80.0)
        val Flows = listOf(Rent, Lunch, LoanRepayment, Shoes)
    }
}
