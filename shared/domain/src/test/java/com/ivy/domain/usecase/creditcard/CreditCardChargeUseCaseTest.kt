package com.ivy.domain.usecase.creditcard

import com.ivy.base.model.AccountType
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class CreditCardChargeUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()

    private val useCase = CreditCardChargeUseCase(
        accountRepository = accountRepository,
        timeConverter = TestTimeConverter(),
    )

    @Test
    fun `a purchase is charged on the charge day of the next month`() {
        card(chargeDay = 15).chargeDateTime(LocalDateTime.of(2026, 10, 3, 18, 30)) shouldBe
            LocalDateTime.of(2026, 11, 15, 18, 30)
    }

    @Test
    fun `a purchase at the end of the year is charged in January`() {
        card(chargeDay = 10).chargeDateTime(LocalDateTime.of(2026, 12, 31, 9, 0)) shouldBe
            LocalDateTime.of(2027, 1, 10, 9, 0)
    }

    @Test
    fun `a charge day past the month's end falls on its last day`() {
        card(chargeDay = 31).chargeDateTime(LocalDateTime.of(2026, 1, 20, 12, 0)) shouldBe
            LocalDateTime.of(2026, 2, 28, 12, 0)
    }

    @Test
    fun `an account that isn't a credit card has no charge date`() {
        card(chargeDay = 15).copy(type = AccountType.BANK)
            .chargeDateTime(LocalDateTime.of(2026, 10, 3, 18, 30)) shouldBe null
    }

    @Test
    fun `a purchase before the statement closes is charged on the next charge day`() {
        card(chargeDay = 5, statementClosingDay = 25).chargeDateTime(LocalDateTime.of(2026, 10, 20, 9, 0)) shouldBe
            LocalDateTime.of(2026, 11, 5, 9, 0)
    }

    @Test
    fun `a purchase on the closing day is in that statement`() {
        card(chargeDay = 5, statementClosingDay = 25).chargeDateTime(LocalDateTime.of(2026, 10, 25, 9, 0)) shouldBe
            LocalDateTime.of(2026, 11, 5, 9, 0)
    }

    @Test
    fun `a purchase after the statement closed is charged a month later`() {
        card(chargeDay = 5, statementClosingDay = 25).chargeDateTime(LocalDateTime.of(2026, 10, 27, 9, 0)) shouldBe
            LocalDateTime.of(2026, 12, 5, 9, 0)
    }

    @Test
    fun `a card can charge in the month its statement closes`() {
        card(chargeDay = 25, statementClosingDay = 5).chargeDateTime(LocalDateTime.of(2026, 11, 3, 9, 0)) shouldBe
            LocalDateTime.of(2026, 11, 25, 9, 0)
    }

    @Test
    fun `a moved charge is that many months away`() {
        val card = card(chargeDay = 5, statementClosingDay = 25)
        val purchase = LocalDateTime.of(2026, 10, 27, 9, 0)

        card.chargeDateTime(purchase, shift = 1) shouldBe LocalDateTime.of(2027, 1, 5, 9, 0)
        card.chargeDateTime(purchase, shift = -1) shouldBe LocalDateTime.of(2026, 11, 5, 9, 0)
    }

    @Test
    fun `a charge can't move to the purchase's day or before it`() {
        val card = card(chargeDay = 5, statementClosingDay = 25)
        val purchase = LocalDateTime.of(2026, 10, 27, 9, 0)

        card.minChargeShift(purchase) shouldBe -1
        card.chargeDateTime(purchase, shift = -3) shouldBe LocalDateTime.of(2026, 11, 5, 9, 0)
        card.minChargeShift(LocalDateTime.of(2026, 11, 5, 9, 0)) shouldBe 0
    }

    @Test
    fun `an expense on a credit card moves to its charge date and keeps its purchase date`() = runTest {
        val card = card(chargeDay = 15)
        coEvery { accountRepository.findById(card.id) } returns card
        val purchase = Instant.parse("2026-10-03T18:30:00Z")

        val dated = useCase.dated(expense(card.id, time = purchase))

        dated.time shouldBe Instant.parse("2026-11-15T18:30:00Z")
        dated.metadata.purchaseDateTime shouldBe purchase
    }

    @Test
    fun `dating an expense again counts from its purchase date`() = runTest {
        val card = card(chargeDay = 15)
        coEvery { accountRepository.findById(card.id) } returns card
        val purchase = Instant.parse("2026-10-03T18:30:00Z")

        val datedTwice = useCase.dated(useCase.dated(expense(card.id, time = purchase)))

        datedTwice.time shouldBe Instant.parse("2026-11-15T18:30:00Z")
        datedTwice.metadata.purchaseDateTime shouldBe purchase
    }

    @Test
    fun `an expense whose charge was moved is dated on the moved charge date`() = runTest {
        val card = card(chargeDay = 15)
        coEvery { accountRepository.findById(card.id) } returns card
        val expense = expense(card.id, time = Instant.parse("2026-10-03T18:30:00Z"))

        val dated = useCase.dated(expense.copy(metadata = expense.metadata.copy(chargeShift = 1)))

        dated.time shouldBe Instant.parse("2026-12-15T18:30:00Z")
    }

    @Test
    fun `a planned expense on a credit card stays on its due date`() = runTest {
        val card = card(chargeDay = 15)
        coEvery { accountRepository.findById(card.id) } returns card
        val planned = expense(card.id, time = Instant.parse("2026-10-03T18:30:00Z"), settled = false)

        useCase.dated(planned) shouldBe planned
    }

    @Test
    fun `income on a credit card stays on its date`() = runTest {
        val card = card(chargeDay = 15)
        coEvery { accountRepository.findById(card.id) } returns card
        val refund: Transaction = Income(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = Instant.parse("2026-10-03T18:30:00Z"),
            settled = true,
            metadata = metadata(),
            tags = emptyList(),
            value = PositiveValue(PositiveDouble.unsafe(10.0), Eur),
            account = card.id,
        )

        useCase.dated(refund) shouldBe refund
    }

    @Test
    fun `an expense on another account stays on its date`() = runTest {
        val bank = card(chargeDay = 15).copy(type = AccountType.BANK)
        coEvery { accountRepository.findById(bank.id) } returns bank
        val expense = expense(bank.id, time = Instant.parse("2026-10-03T18:30:00Z"))

        useCase.dated(expense) shouldBe expense
    }

    private fun card(chargeDay: Int, statementClosingDay: Int? = null) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Card"),
        asset = Eur,
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.CREDIT_CARD,
        archived = false,
        creditLimit = PositiveDouble.unsafe(1500.0),
        chargeDay = chargeDay,
        statementClosingDay = statementClosingDay,
        orderNum = 0.0,
    )

    private fun expense(account: AccountId, time: Instant, settled: Boolean = true) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = time,
        settled = settled,
        metadata = metadata(),
        tags = emptyList(),
        value = PositiveValue(PositiveDouble.unsafe(42.0), Eur),
        account = account,
    )

    private fun metadata() = TransactionMetadata(
        recurringRuleId = null,
        paidForDateTime = null,
        loanId = null,
        loanRecordId = null,
        purchaseDateTime = null,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
    }
}
