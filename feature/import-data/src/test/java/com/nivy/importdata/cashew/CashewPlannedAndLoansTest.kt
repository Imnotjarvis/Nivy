package com.nivy.importdata.cashew

import com.nivy.base.model.IntervalType
import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.LoanType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Expense
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.Income
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.importdata.cashew.LeftOut.Reason
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CashewPlannedAndLoansTest {

    @Test
    fun `a subscription is a planned payment from its next occurrence, its paid ones linked to it`() {
        // given two paid months and the next one, as Cashew keys a series
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Streaming")
            subscription("s1", date = "2026-09-06T10:00:00Z", due = "2026-09-05T08:00:00Z")
            subscription("s1::predict::1", date = "2026-10-05T08:00:00Z", due = "2026-10-05T08:00:00Z")
            subscription("s1::predict::2", date = "2026-11-05T08:00:00Z", paid = false)
        }

        // then
        val rule = plan.plannedPayments.single()
        rule.startDate shouldBe Instant.parse("2026-11-05T08:00:00Z")
        rule.interval?.type shouldBe IntervalType.MONTH
        rule.interval?.n?.value shouldBe 1
        rule.type shouldBe TransactionType.EXPENSE
        rule.amount.value shouldBe 9.99
        rule.title?.value shouldBe "Music"
        rule.category shouldBe plan.categories.single().id
        plan.transactions.map { it.metadata.recurringRuleId } shouldBe listOf(rule.id.value, rule.id.value)
        plan.transactions.map { it.metadata.paidForDateTime } shouldBe listOf(
            Instant.parse("2026-09-05T08:00:00Z"),
            Instant.parse("2026-10-05T08:00:00Z"),
        )
        plan.transactions.map { it.time } shouldBe listOf(
            Instant.parse("2026-09-06T10:00:00Z"),
            Instant.parse("2026-10-05T08:00:00Z"),
        )
        plan.found shouldBe 2
    }

    @Test
    fun `a subscription that ends in Cashew repeats in Nivy, and is listed`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Streaming")
            subscription("s1", date = "2026-11-05T08:00:00Z", paid = false, end = "2027-03-01T00:00:00Z")
        }

        // then
        plan.plannedPayments.single().interval?.type shouldBe IntervalType.MONTH
        plan.leftOut shouldBe listOf(LeftOut(Reason.EndsInCashew, listOf(LeftOut.Item("Music", LocalDate.of(2027, 3, 1)))))
    }

    @Test
    fun `a series with nothing left to pay keeps its paid ones linked, without a planned payment`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Streaming")
            subscription("s1", date = "2026-09-05T08:00:00Z")
            subscription("s1::predict::1", date = "2026-10-05T08:00:00Z")
        }

        // then
        plan.plannedPayments.shouldBeEmpty()
        val rules = plan.transactions.map { it.metadata.recurringRuleId }.distinct()
        rules.size shouldBe 1
        rules.single() shouldBe plan.transactions.first().metadata.recurringRuleId
    }

    @Test
    fun `an upcoming payment is a one-time planned payment, until it's paid`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Bills")
            transaction(
                "u1", -80.0, name = "Dentist", type = CashewType.Upcoming, paid = false,
                date = "2026-12-01T09:00:00Z",
            )
            transaction(
                "u2", -45.0, name = "Plumber", type = CashewType.Upcoming,
                date = "2026-10-03T09:00:00Z", dueDate = "2026-10-02T09:00:00Z",
            )
        }

        // then
        with(plan.plannedPayments.single()) {
            interval shouldBe null
            startDate shouldBe Instant.parse("2026-12-01T09:00:00Z")
            title?.value shouldBe "Dentist"
        }
        with(plan.transactions.single() as Expense) {
            title?.value shouldBe "Plumber"
            metadata.paidForDateTime shouldBe Instant.parse("2026-10-02T09:00:00Z")
            (metadata.recurringRuleId != null) shouldBe true
        }
    }

    @Test
    fun `a planned transfer isn't planned, and is listed`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            wallet("w2", "Savings")
            transaction("in", 100.0, wallet = "w2", category = "0", paid = false, type = CashewType.Repetitive)
            transaction(
                "out", -100.0, wallet = "w1", category = "0", paired = "in", paid = false,
                type = CashewType.Repetitive, name = "Savings",
            )
        }

        // then
        plan.plannedPayments.shouldBeEmpty()
        plan.leftOut.single().reason shouldBe Reason.PlannedTransfer
    }

    @Test
    fun `money lent is a loan with the expense that lent it, and collected in full, a loan paid back`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Loans", colour = "0xff9c27b0")
            transaction("l1", -200.0, name = "Anna", type = CashewType.Credit, date = "2026-09-01T10:00:00Z")
            transaction(
                "l2", -50.0, name = "Marco", type = CashewType.Credit, paid = false,
                date = "2026-08-01T10:00:00Z", modified = "2026-09-15T18:00:00Z",
            )
            transaction("d1", 300.0, name = "Bank of Mum", type = CashewType.Debt, date = "2026-07-01T10:00:00Z")
        }

        // then
        plan.loans.map { Triple(it.name.value, it.type, it.amount.value) } shouldBe listOf(
            Triple("Bank of Mum", LoanType.BORROW, 300.0),
            Triple("Marco", LoanType.LEND, 50.0),
            Triple("Anna", LoanType.LEND, 200.0),
        )
        val (mum, marco, anna) = plan.loans
        // Marco paid it all back: no money moved in Nivy, as none counts in Cashew any more
        with(plan.loanRecords.single()) {
            loan shouldBe marco.id
            type shouldBe LoanRecordType.DECREASE
            amount.value shouldBe 50.0
            account shouldBe null
            dateTime shouldBe Instant.parse("2026-09-15T18:00:00Z")
        }
        plan.transactions.map { it.javaClass.simpleName to it.metadata.loanId } shouldBe listOf(
            "Income" to mum.id.value,
            "Expense" to anna.id.value,
        )
        anna.color.value shouldBe 0xff9c27b0.toInt()
        plan.found shouldBe 2
    }

    @Test
    fun `a long-term loan is its first amount, with records of more lent and of what was paid back`() {
        // given money lent to Anna, part of it paid back, then more lent
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Loans")
            objective("o1", "Anna", loan = true, income = true)
            transaction("r1", -500.0, name = "Initial Record", loan = "o1", date = "2026-06-01T10:00:00Z")
            transaction("r2", 200.0, name = "First back", loan = "o1", date = "2026-07-01T10:00:00Z")
            transaction("r3", -100.0, loan = "o1", date = "2026-08-01T10:00:00Z")
        }

        // then
        val loan = plan.loans.single()
        loan.name.value shouldBe "Anna"
        loan.type shouldBe LoanType.LEND
        loan.amount.value shouldBe 500.0
        plan.loanRecords.map { Triple(it.type, it.amount.value, it.note?.value) } shouldBe listOf(
            Triple(LoanRecordType.DECREASE, 200.0, "First back"),
            Triple(LoanRecordType.INCREASE, 100.0, null),
        )
        plan.transactions.map { it.metadata.loanId } shouldBe List(3) { loan.id.value }
        plan.transactions.map { it.metadata.loanRecordId } shouldBe
            listOf(null) + plan.loanRecords.map { it.id.value }
        plan.transactions.first().title?.value shouldBe "Anna"
        (plan.transactions[1] is Income) shouldBe true
    }

    @Test
    fun `a loan payment in another currency is converted at Cashew's rates`() {
        // when
        val plan = plan {
            wallet("w1", "Bank", currency = "eur")
            wallet("w2", "Dollars", currency = "usd")
            category("c1", "Loans")
            objective("o1", "Anna", loan = true, income = true)
            transaction("r1", -500.0, loan = "o1", date = "2026-06-01T10:00:00Z")
            transaction("r2", 110.0, wallet = "w2", loan = "o1", date = "2026-07-01T10:00:00Z")
            settings("""{"cachedCurrencyExchange":{"eur":0.9,"usd":1}}""")
        }

        // then
        plan.loanRecords.single().convertedAmount?.value shouldBe (99.0 plusOrMinus 1e-9)
    }

    @Test
    fun `a goal's transactions are tagged with its name`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            category("c1", "Savings")
            objective("g1", "Holiday")
            transaction("t1", -150.0, goal = "g1")
        }

        // then
        plan.tags.single().name.value shouldBe "Holiday"
        plan.tagLinks.single().associatedId.value shouldBe plan.transactions.single().id.value
    }

    @Test
    fun `monthly budgets come over with their categories and wallets, the others are listed`() {
        // when
        val plan = plan {
            wallet("w1", "Bank")
            wallet("w2", "Cash")
            category("c1", "Dining", order = 0)
            category("c2", "Coffee", main = "c1")
            category("c3", "Travel", order = 1)
            budget("b1", "Food", 300.0, categories = """["c2"]""", wallets = """["w2"]""")
            budget("b2", "All but travel", 900.0, excluded = """["c3"]""")
            budget("b3", "Everything", 1500.0)
            budget("b4", "Weekly", 50.0, repeat = CashewRepeat.Weekly)
            budget("b5", "Salary", 3000.0, income = true)
            categoryLimit("l1", budget = "b3", category = "c1")
        }

        // then
        val (dining, travel) = plan.categories
        plan.budgets.map { Triple(it.name.value, it.categories, it.accounts) } shouldBe listOf(
            Triple("Food", listOf(dining.id), listOf(plan.accounts.last().id)),
            Triple("All but travel", listOf(dining.id), emptyList()),
            Triple("Everything", emptyList(), emptyList()),
        )
        travel.name.value shouldBe "Travel"
        plan.leftOut shouldBe listOf(
            LeftOut(Reason.CategoryLimits, listOf(LeftOut.Item("Everything"))),
            LeftOut(Reason.Budget, listOf(LeftOut.Item("Weekly"), LeftOut.Item("Salary"))),
        )
    }

    @Test
    fun `a budget in another currency than the base one is converted at Cashew's rates`() {
        // when
        val plan = plan {
            wallet("w1", "Dollars", currency = "usd")
            budget("b1", "Food", 300.0, wallet = "w1")
            budget("b2", "Fun", 100.0, wallet = "w1")
            settings("""{"cachedCurrencyExchange":{"eur":0.9}}""")
        }

        // then the dollar is one dollar when Cashew has no rate for it, as in Cashew
        plan.budgets[0].amount.value shouldBe (270.0 plusOrMinus 1e-9)
        plan.budgets[1].amount.value shouldBe (90.0 plusOrMinus 1e-9)
    }

    @Test
    fun `rates set by hand in Cashew are rates entered by hand, unless one is already`() {
        // given
        val franc = ExchangeRate(
            baseCurrency = AssetCode.EUR,
            currency = AssetCode.unsafe("CHF"),
            rate = PositiveDouble.unsafe(0.95),
            manualOverride = true,
        )

        // when
        val plan = plan(wallet(manualRates = listOf(franc))) {
            wallet("w1", "Bank", currency = "eur")
            wallet("w2", "Dollars", currency = "usd")
            wallet("w3", "Francs", currency = "chf")
            wallet("w4", "Pounds", currency = "gbp")
            settings(
                """{"cachedCurrencyExchange":{"eur":0.9,"usd":1,"gbp":0.8},""" +
                    """"customCurrencyAmounts":{"usd":1.2,"chf":0.85}}"""
            )
        }

        // then only the dollar's: the franc has one already, the pound's was downloaded
        plan.rates.map { it.currency.code to it.manualOverride } shouldBe listOf("USD" to true)
        plan.rates.single().rate.value shouldBe (1.2 / 0.9 plusOrMinus 1e-9)
    }

    private fun CashewFile.subscription(
        pk: String,
        date: String,
        due: String? = null,
        paid: Boolean = true,
        end: String? = null,
    ) = transaction(
            pk, -9.99, name = "Music", type = CashewType.Subscription, repeat = CashewRepeat.Monthly,
            repeatEvery = 1, date = date, dueDate = due, paid = paid, endDate = end,
        )
}
