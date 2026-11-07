package com.nivy.domain.usecase.account

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.AccountValue
import com.nivy.data.model.AccountValueId
import com.nivy.data.model.Expense
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NonNegativeDouble
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountValueRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.wallet.domain.pure.transaction.AccountValueFunctions
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class AccountBalanceUseCaseTest {
    private val transactionRepository = mockk<TransactionRepository>()
    private val accountValueRepository = mockk<AccountValueRepository>()
    private val timeProvider = mockk<TimeProvider> { every { utcNow() } returns Now }

    private val useCase = AccountBalanceUseCase(
        transactionRepository,
        accountValueRepository,
        timeProvider,
        TestDispatchersProvider,
    )

    @Test
    fun `an investment account is worth its last value, with the money moved in since`() = runTest {
        // given: 1,000 in, valued at 1,300 on day 5, then 500 more in
        transactions(transferIn(1_000.0, day = 1), transferIn(500.0, day = 10))
        values(value(1_200.0, day = 3), value(1_300.0, day = 5))

        // when
        val output = useCase.balance(Investment)

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
        val output = useCase.balance(Investment)

        // then: a loss of 100 on the 1,000 put in, and 200 taken out since
        output.balance.toDouble() shouldBe 700.0
        output.gain?.toDouble() shouldBe -100.0
    }

    @Test
    fun `the cash entered with a value counts at its amount, gaining nothing`() = runTest {
        // given: 1,000 in, of which 300 bought what's now worth 330, and 700 left as cash
        transactions(transferIn(1_000.0, day = 1))
        values(value(330.0, day = 5, cash = 700.0))

        // when
        val output = useCase.balance(Investment)

        // then
        output.balance.toDouble() shouldBe 1_030.0
        output.gain?.toDouble() shouldBe 30.0
        output.cash?.toDouble() shouldBe 700.0
    }

    @Test
    fun `money moved in after a value with cash waits as cash`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1), transferIn(300.0, day = 10))
        values(value(330.0, day = 5, cash = 700.0))

        // when
        val output = useCase.balance(Investment)

        // then
        output.balance.toDouble() shouldBe 1_330.0
        output.gain?.toDouble() shouldBe 30.0
        output.cash?.toDouble() shouldBe 1_000.0
    }

    @Test
    fun `money taken out beyond the cash comes from the investments`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1), transferOut(900.0, day = 10))
        values(value(330.0, day = 5, cash = 700.0))

        // when
        val output = useCase.balance(Investment)

        // then
        output.balance.toDouble() shouldBe 130.0
        output.cash?.toDouble() shouldBe 0.0
    }

    @Test
    fun `without cash in the last value all the money in is invested`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1), transferIn(100.0, day = 10))
        values(value(1_050.0, day = 5))

        // when
        val output = useCase.balance(Investment)

        // then
        output.balance.toDouble() shouldBe 1_150.0
        output.cash shouldBe null
    }

    @Test
    fun `without a value the balance is what the transactions add up to`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1))
        values()

        // when
        val output = useCase.balance(Investment)

        // then
        output.balance.toDouble() shouldBe 1_000.0
        output.gain shouldBe null
    }

    @Test
    fun `transactions dated after now don't count yet`() = runTest {
        // given: an expense charged later, after now (day 31)
        transactions(transferIn(1_000.0, day = 1), expense(40.0, day = 40))

        // when
        val output = useCase.balance(Investment.copy(type = AccountType.BANK))

        // then
        output.balance.toDouble() shouldBe 1_000.0
    }

    @Test
    fun `other accounts don't look for values`() = runTest {
        // given
        transactions(transferIn(1_000.0, day = 1), expense(40.0, day = 2))

        // when
        val output = useCase.balance(Investment.copy(type = AccountType.BANK))

        // then
        output.balance.toDouble() shouldBe 960.0
        output.gain shouldBe null
        coVerify(exactly = 0) { accountValueRepository.findByAccount(any()) }
    }

    // The database sums the transactions dated in the range asked, as they're counted one by one.
    private fun transactions(vararg transactions: Transaction) {
        coEvery { transactionRepository.balance(any(), any(), any()) } answers {
            // AccountId, a value class, comes as the UUID it wraps.
            val account = firstArg<UUID>()
            val (from, to) = secondArg<Instant>() to thirdArg<Instant>()
            transactions.filter { it.time in from..to }
                .sumOf { AccountValueFunctions.balance(it, account) }
                .toDouble()
        }
    }

    private fun values(vararg values: AccountValue) {
        coEvery { accountValueRepository.findByAccount(Investment.id) } returns values.toList()
    }

    private fun value(amount: Double, day: Long, cash: Double? = null) = AccountValue(
        id = AccountValueId(UUID.randomUUID()),
        account = Investment.id,
        time = day(day),
        value = NonNegativeDouble.unsafe(amount),
        cash = cash?.let(NonNegativeDouble::unsafe),
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
