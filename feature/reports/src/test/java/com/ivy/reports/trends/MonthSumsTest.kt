package com.ivy.reports.trends

import com.ivy.base.model.AccountType
import com.ivy.data.model.Account
import com.ivy.data.model.AccountValue
import com.ivy.data.model.AccountValueId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NonNegativeDouble
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class MonthSumsTest {
    private val bank = AccountId(UUID.randomUUID())
    private val savings = AccountId(UUID.randomUUID())
    private val excluded = AccountId(UUID.randomUUID())
    private val included = setOf(bank, savings)

    private val october = range("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")
    private val november = range("2026-11-01T00:00:00Z", "2026-11-30T23:59:59Z")

    @Test
    fun `income and expenses count in their month`() {
        val sums = monthSums(
            transactions = listOf(
                income(bank, 1000.0, "2026-10-05T10:00:00Z"),
                expense(bank, 200.0, "2026-10-20T10:00:00Z"),
                expense(bank, 50.0, "2026-11-02T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(october, november),
            now = Instant.parse("2026-11-15T00:00:00Z"),
        )

        sums.map { it.income[Eur] } shouldBe listOf(1000.0, null)
        sums.map { it.expense[Eur] } shouldBe listOf(200.0, 50.0)
    }

    @Test
    fun `net worth is what the accounts hold at each month's end`() {
        val sums = monthSums(
            transactions = listOf(
                income(bank, 1000.0, "2026-10-05T10:00:00Z"),
                expense(bank, 200.0, "2026-10-20T10:00:00Z"),
                expense(bank, 50.0, "2026-11-02T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(october, november),
            now = Instant.parse("2026-11-15T00:00:00Z"),
        )

        sums.map { it.netWorth[Eur] } shouldBe listOf(800.0, 750.0)
    }

    @Test
    fun `the current month's net worth leaves out what's dated after now`() {
        val sums = monthSums(
            transactions = listOf(
                income(bank, 1000.0, "2026-11-01T10:00:00Z"),
                // A credit card purchase charged later this month.
                expense(bank, 300.0, "2026-11-20T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(november),
            now = Instant.parse("2026-11-15T00:00:00Z"),
        )

        sums.single().netWorth[Eur] shouldBe 1000.0
        sums.single().expense[Eur] shouldBe 300.0
    }

    @Test
    fun `transfers between counted accounts move money without changing anything`() {
        val sums = monthSums(
            transactions = listOf(
                income(bank, 1000.0, "2026-10-05T10:00:00Z"),
                transfer(bank, savings, 400.0, "2026-10-06T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(october),
            now = Instant.parse("2026-11-15T00:00:00Z"),
        )

        sums.single().income[Eur] shouldBe 1000.0
        sums.single().expense[Eur] shouldBe null
        sums.single().netWorth[Eur] shouldBe 1000.0
    }

    @Test
    fun `accounts left out of the balance don't count`() {
        val sums = monthSums(
            transactions = listOf(
                income(excluded, 500.0, "2026-10-05T10:00:00Z"),
                transfer(bank, excluded, 100.0, "2026-10-06T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(october),
            now = Instant.parse("2026-11-15T00:00:00Z"),
        )

        sums.single().income[Eur] shouldBe null
        sums.single().netWorth[Eur] shouldBe -100.0
    }

    @Test
    fun `planned transactions don't count until paid`() {
        val sums = monthSums(
            transactions = listOf(expense(bank, 80.0, "2026-10-10T10:00:00Z", settled = false)),
            accounts = included,
            months = listOf(october),
            now = Instant.parse("2026-11-15T00:00:00Z"),
        )

        sums.single().expense[Eur] shouldBe null
        sums.single().netWorth[Eur] shouldBe null
    }

    private fun range(from: String, to: String) = ClosedTimeRange(Instant.parse(from), Instant.parse(to))

    @Test
    fun `an investment account counts at its value from the month it was entered`() {
        // given: 1,000 into the portfolio in October, valued at 1,100 in November, 200 more after
        val portfolio = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Portfolio"),
            asset = Eur,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.INVESTMENT,
            archived = false,
            orderNum = 0.0,
        )
        val value = AccountValue(
            id = AccountValueId(UUID.randomUUID()),
            account = portfolio.id,
            time = Instant.parse("2026-11-05T18:00:00Z"),
            value = NonNegativeDouble.unsafe(1_100.0),
            cash = null,
        )

        // when
        val sums = monthSums(
            transactions = listOf(
                transfer(bank, portfolio.id, 1_000.0, "2026-10-10T10:00:00Z"),
                transfer(bank, portfolio.id, 200.0, "2026-11-10T10:00:00Z"),
            ),
            accounts = included + portfolio.id,
            months = listOf(october, november),
            now = Instant.parse("2026-11-15T00:00:00Z"),
            values = mapOf(portfolio to listOf(value)),
        )

        // then: October holds what went in; November the value, its 100 gain and the 200 since,
        // less the 1,200 the bank sent
        sums.map { it.netWorth[Eur] } shouldBe listOf(0.0, 100.0)
    }

    private fun income(account: AccountId, amount: Double, time: String): Transaction = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = eur(amount),
        account = account,
    )

    private fun expense(
        account: AccountId,
        amount: Double,
        time: String,
        settled: Boolean = true,
    ): Transaction = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = settled,
        metadata = Metadata,
        tags = emptyList(),
        value = eur(amount),
        account = account,
    )

    private fun transfer(from: AccountId, to: AccountId, amount: Double, time: String): Transaction = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse(time),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        fromAccount = from,
        fromValue = eur(amount),
        toAccount = to,
        toValue = eur(amount),
    )

    private fun eur(amount: Double) = PositiveValue(PositiveDouble.unsafe(amount), Eur)

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )
    }
}
