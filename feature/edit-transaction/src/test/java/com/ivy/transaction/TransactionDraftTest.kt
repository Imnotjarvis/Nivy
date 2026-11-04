package com.ivy.transaction

import com.ivy.base.model.AccountType
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.mapper.TransactionMapper
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TransactionDraftTest {

    private val bank = account("Bank", Eur)
    private val dollars = account("Dollars", Usd)
    private val card = account("Card", Eur).copy(type = AccountType.CREDIT_CARD, chargeDay = 5)

    private val mapper = TransactionMapper(
        mockk<AccountRepository> {
            listOf(bank, dollars, card).forEach { coEvery { findById(it.id) } returns it }
        }
    )

    @Test
    fun `a credit card expense opens on the day it was bought, with its charge move`() {
        val bought = Instant.parse("2026-11-15T12:00:00Z")
        val expense = expense(card, 30.0, time = Instant.parse("2026-12-05T12:00:00Z")).copy(
            metadata = Metadata.copy(purchaseDateTime = bought, chargeShift = 1),
        )

        val draft = expense.toDraft()

        draft.dateTime shouldBe bought
        draft.dueDate shouldBe null
        draft.chargeShift shouldBe 1
    }

    @Test
    fun `a planned payment opens with its due date and no date paid`() {
        val due = Instant.parse("2026-12-01T09:00:00Z")

        val draft = expense(bank, 30.0, time = due).copy(settled = false).toDraft()

        draft.dateTime shouldBe null
        draft.dueDate shouldBe due
    }

    @Test
    fun `a transfer opens with what each side moved`() {
        val transfer = Transfer(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = Instant.parse("2026-11-15T12:00:00Z"),
            settled = true,
            metadata = Metadata,
            tags = emptyList(),
            fromAccount = bank.id,
            fromValue = PositiveValue(PositiveDouble.unsafe(100.0), Eur),
            toAccount = dollars.id,
            toValue = PositiveValue(PositiveDouble.unsafe(110.0), Usd),
        )

        val draft = transfer.toDraft()

        draft.type shouldBe TransactionType.TRANSFER
        draft.accountId shouldBe bank.id.value
        draft.amount shouldBe 100.0
        draft.toAccountId shouldBe dollars.id.value
        draft.toAmount shouldBe 110.0
    }

    @Test
    fun `an unchanged draft saves as the transaction it was opened from`() = runTest {
        val transfer = Transfer(
            id = TransactionId(UUID.randomUUID()),
            title = NotBlankTrimmedString.unsafe("Savings"),
            description = NotBlankTrimmedString.unsafe("Monthly"),
            category = CategoryId(UUID.randomUUID()),
            time = Instant.parse("2026-11-15T12:00:00Z"),
            settled = true,
            metadata = Metadata,
            tags = emptyList(),
            fromAccount = bank.id,
            fromValue = PositiveValue(PositiveDouble.unsafe(100.0), Eur),
            toAccount = dollars.id,
            toValue = PositiveValue(PositiveDouble.unsafe(110.0), Usd),
        )
        val expense = expense(bank, 30.0, time = Instant.parse("2026-11-15T12:00:00Z"))

        transfer.toDraft().toDomain(mapper) shouldBe transfer
        expense.toDraft().toDomain(mapper) shouldBe expense
    }

    @Test
    fun `a draft that isn't a valid transaction yet isn't saved`() = runTest {
        val now = Instant.parse("2026-11-15T12:00:00Z")
        val noAmount = TransactionDraft(bank.id.value, TransactionType.EXPENSE, amount = 0.0, dateTime = now)
        val noDestination = TransactionDraft(bank.id.value, TransactionType.TRANSFER, amount = 10.0, dateTime = now)
        val toItself = noDestination.copy(toAccountId = bank.id.value)

        noAmount.toDomain(mapper) shouldBe null
        noDestination.toDomain(mapper) shouldBe null
        toItself.toDomain(mapper) shouldBe null
    }

    private fun expense(account: Account, amount: Double, time: Instant) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = NotBlankTrimmedString.unsafe("Lunch"),
        description = null,
        category = CategoryId(UUID.randomUUID()),
        time = time,
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(amount), account.asset),
        account = account.id,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")

        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            creditLimit = null,
            chargeDay = null,
            statementClosingDay = null,
            orderNum = 0.0,
        )
    }
}
