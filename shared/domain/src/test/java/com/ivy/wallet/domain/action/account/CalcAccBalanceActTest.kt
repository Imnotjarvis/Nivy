package com.ivy.wallet.domain.action.account

import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountValue
import com.ivy.data.model.AccountValueId
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NonNegativeDouble
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountValueRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class CalcAccBalanceActTest {
    private val accTrnsAct = mockk<AccTrnsAct>()
    private val accountValueRepository = mockk<AccountValueRepository>()
    private val timeProvider = mockk<TimeProvider> { every { utcNow() } returns Now }

    private val act = CalcAccBalanceAct(accTrnsAct, accountValueRepository, timeProvider)

    @Test
    fun `an investment account is worth its last value, with the money moved in since`() = runTest {
        // given: 1,000 in, valued at 1,300 on day 5, then 500 more in
        transactions(transferIn(1_000.0, day = 1), transferIn(500.0, day = 10))
        values(value(1_200.0, day = 3), value(1_300.0, day = 5))

        // when
        val output = act(CalcAccBalanceAct.Input(Investment))

        // then
        output.balance.toDouble() shouldBe 1_800.0
        output.gain?.toDouble() shouldBe 300.0
    }

    @Test
    fun `money taken out after the value comes off it`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1), transferOut(200.0, day = 10))
        values(value(900.0, day = 5))

        // when
        val output = act(CalcAccBalanceAct.Input(Investment))

        // then: a loss of 100 on the 1,000 put in, and 200 taken out since
        output.balance.toDouble() shouldBe 700.0
        output.gain?.toDouble() shouldBe -100.0
    }

    @Test
    fun `without a value the balance is what the transactions add up to`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1))
        values()

        // when
        val output = act(CalcAccBalanceAct.Input(Investment))

        // then
        output.balance.toDouble() shouldBe 1_000.0
        output.gain shouldBe null
    }

    @Test
    fun `other accounts don't look for values`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1), expense(40.0, day = 2))

        // when
        val output = act(CalcAccBalanceAct.Input(Investment.copy(type = AccountType.BANK)))

        // then
        output.balance.toDouble() shouldBe 960.0
        output.gain shouldBe null
        coVerify(exactly = 0) { accountValueRepository.findByAccount(any()) }
    }

    private fun transactions(vararg transactions: Transaction) {
        coEvery { accTrnsAct(any()) } returns transactions.toList()
    }

    private fun values(vararg values: AccountValue) {
        coEvery { accountValueRepository.findByAccount(Investment.id) } returns values.toList()
    }

    private fun value(amount: Double, day: Long) = AccountValue(
        id = AccountValueId(UUID.randomUUID()),
        account = Investment.id,
        time = day(day),
        value = NonNegativeDouble.unsafe(amount),
    )

    private fun transferIn(amount: Double, day: Long) = transfer(amount, day, from = Main, to = Investment.id)

    private fun transferOut(amount: Double, day: Long) = transfer(amount, day, from = Investment.id, to = Main)

    private fun transfer(amount: Double, day: Long, from: AccountId, to: AccountId) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = day(day),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        fromAccount = from,
        fromValue = eur(amount),
        toAccount = to,
        toValue = eur(amount),
    )

    private fun expense(amount: Double, day: Long) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = day(day),
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = eur(amount),
        account = Investment.id,
    )

    private fun eur(amount: Double) = PositiveValue(PositiveDouble.unsafe(amount), Eur)

    private fun day(day: Long): Instant = Start.plusSeconds(day * SecondsInDay)

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Start: Instant = Instant.parse("2026-10-01T10:00:00Z")
        val Now: Instant = Instant.parse("2026-11-01T10:00:00Z")
        const val SecondsInDay = 86_400L
        val Main = AccountId(UUID.randomUUID())
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )
        val Investment = Account(
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
    }
}
