package com.nivy.domain.usecase.exchange

import arrow.core.None
import arrow.core.Some
import com.nivy.data.model.AccountId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class BaseCurrencyRatesTest {
    private val exchangeUseCase = mockk<ExchangeUseCase> {
        coEvery { convert(value(1.0, Usd), Eur) } returns Some(value(0.9, Eur))
        coEvery { convert(value(1.0, Jpy), Eur) } returns None
    }
    private val rates = BaseCurrencyRates(exchangeUseCase, Eur)

    @Test
    fun `amounts in the base currency stay as they are, others count at their rate, read once`() = runTest {
        rates.inBase(value(12.5, Eur)) shouldBe 12.5
        rates.inBase(100.0, Usd) shouldBe 90.0
        rates.inBase(value(50.0, Usd)) shouldBe 45.0

        coVerify(exactly = 1) { exchangeUseCase.convert(value(1.0, Usd), Eur) }
        coVerify(exactly = 0) { exchangeUseCase.convert(value(1.0, Eur), any()) }
    }

    @Test
    fun `without a rate, an amount counts for nothing`() = runTest {
        rates.factor(Jpy) shouldBe null
        rates.inBase(1_000.0, Jpy) shouldBe 0.0
    }

    @Test
    fun `a balance, below zero too, counts at its rate, and is null without one`() = runTest {
        rates.inBaseOrNull(-200.0, Usd) shouldBe -180.0
        rates.inBaseOrNull(-200.0, Jpy) shouldBe null
    }

    @Test
    fun `income counts up, expenses down, and a transfer stays within the wallet`() = runTest {
        rates.signed(income(20.0)) shouldBe 20.0
        rates.signed(expense(7.5)) shouldBe -7.5
        rates.signed(transfer(30.0)) shouldBe 0.0
    }

    private fun income(amount: Double) = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.EPOCH,
        settled = true,
        metadata = Metadata,
        value = value(amount, Eur),
        account = Account,
    )

    private fun expense(amount: Double) = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.EPOCH,
        settled = true,
        metadata = Metadata,
        value = value(amount, Eur),
        account = Account,
    )

    private fun transfer(amount: Double) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.EPOCH,
        settled = true,
        metadata = Metadata,
        fromAccount = Account,
        fromValue = value(amount, Eur),
        toAccount = AccountId(UUID.randomUUID()),
        toValue = value(amount, Eur),
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")
        val Jpy = AssetCode.unsafe("JPY")
        val Account = AccountId(UUID.randomUUID())
        val Metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )

        fun value(amount: Double, asset: AssetCode) = PositiveValue(PositiveDouble.unsafe(amount), asset)
    }
}
