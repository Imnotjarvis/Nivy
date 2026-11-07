package com.nivy.reports.trends

import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
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
        )

        sums.map { it.income[Eur] } shouldBe listOf(1000.0, null)
        sums.map { it.expense[Eur] } shouldBe listOf(200.0, 50.0)
    }

    @Test
    fun `transfers aren't income or expenses`() {
        val sums = monthSums(
            transactions = listOf(
                income(bank, 1000.0, "2026-10-05T10:00:00Z"),
                transfer(bank, savings, 400.0, "2026-10-06T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(october),
        )

        sums.single().income[Eur] shouldBe 1000.0
        sums.single().expense[Eur] shouldBe null
    }

    @Test
    fun `accounts left out of the balance don't count`() {
        val sums = monthSums(
            transactions = listOf(
                income(excluded, 500.0, "2026-10-05T10:00:00Z"),
                expense(excluded, 30.0, "2026-10-07T10:00:00Z"),
            ),
            accounts = included,
            months = listOf(october),
        )

        sums.single().income[Eur] shouldBe null
        sums.single().expense[Eur] shouldBe null
    }

    @Test
    fun `planned transactions don't count until paid`() {
        val sums = monthSums(
            transactions = listOf(expense(bank, 80.0, "2026-10-10T10:00:00Z", settled = false)),
            accounts = included,
            months = listOf(october),
        )

        sums.single().expense[Eur] shouldBe null
    }

    private fun range(from: String, to: String) = ClosedTimeRange(Instant.parse(from), Instant.parse(to))

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
