package com.ivy.domain.usecase.transaction

import arrow.core.None
import arrow.core.Some
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TransactionAmountsUseCaseTest {
    private val exchangeUseCase = mockk<ExchangeUseCase>()
    private val useCase = TransactionAmountsUseCase(exchangeUseCase)

    init {
        coEvery { exchangeUseCase.convert(any<PositiveValue>(), Usd) } answers {
            val value = firstArg<PositiveValue>()
            if (value.asset == Usd) Some(value) else None
        }
        coEvery { exchangeUseCase.convert(value(10.0, Eur), Usd) } returns Some(value(11.0, Usd))
    }

    @Test
    fun `the total converts each amount`() = runTest {
        // when
        val total = useCase.total(listOf(expense(10.0, Eur), income(5.0, Usd)), Usd)

        // then
        total shouldBe 16.0
    }

    @Test
    fun `without a rate the amount counts unconverted`() = runTest {
        // when
        val total = useCase.total(listOf(expense(7.0, Bgn)), Usd)

        // then
        total shouldBe 7.0
    }

    @Test
    fun `the balance is income minus expenses, transfers aside`() = runTest {
        // when
        val balance = useCase.balance(
            listOf(income(100.0, Usd), expense(10.0, Eur), transfer(50.0, Usd)),
            Usd
        )

        // then
        balance shouldBe 89.0
    }

    private fun value(amount: Double, asset: AssetCode) =
        PositiveValue(PositiveDouble.unsafe(amount), asset)

    private fun income(amount: Double, asset: AssetCode) = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Time,
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = value(amount, asset),
        account = AccountId(UUID.randomUUID()),
    )

    private fun expense(amount: Double, asset: AssetCode) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Time,
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        value = value(amount, asset),
        account = AccountId(UUID.randomUUID()),
    )

    private fun transfer(amount: Double, asset: AssetCode) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Time,
        settled = true,
        metadata = Metadata,
        tags = emptyList(),
        fromAccount = AccountId(UUID.randomUUID()),
        fromValue = value(amount, asset),
        toAccount = AccountId(UUID.randomUUID()),
        toValue = value(amount, asset),
    )

    private companion object {
        val Usd = AssetCode.unsafe("USD")
        val Eur = AssetCode.unsafe("EUR")
        val Bgn = AssetCode.unsafe("BGN")
        val Time: Instant = Instant.parse("2026-10-01T09:00:00Z")
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
        )
    }
}
