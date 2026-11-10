package com.nivy.transaction

import arrow.core.Some
import com.nivy.base.model.AccountType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class RateCardTest {
    private val card = RateCard(
        TransferExchangeRate(
            currencyRepository = mockk<CurrencyRepository> { coEvery { getBaseCurrency() } returns Eur },
            exchangeUseCase = mockk<ExchangeUseCase> {
                // 1 EUR = 2 USD
                coEvery { convert(any<PositiveValue>(), Usd) } answers {
                    Some(PositiveValue(PositiveDouble.unsafe(firstArg<PositiveValue>().amount.value * 2.0), Usd))
                }
            },
        )
    )

    @Test
    fun `a new transfer opened on its destination shows the market rate`() = runTest {
        card.show(transfer(amount = 0.0, toAmount = 0.0), account = Bank, toAccount = Dollars, currency = "EUR")

        card.state.showCard shouldBe true
        card.state.exchangeRate shouldBe 2.0
    }

    @Test
    fun `a transfer with its amounts shows the rate between them`() = runTest {
        card.show(transfer(amount = 100.0, toAmount = 230.0), account = Bank, toAccount = Dollars, currency = "EUR")

        card.state.exchangeRate shouldBe 2.3
        card.state.convertedAmount shouldBe 230.0
    }

    @Test
    fun `the amount entered is converted at the rate shown`() = runTest {
        card.show(transfer(amount = 0.0, toAmount = 0.0), account = Bank, toAccount = Dollars, currency = "EUR")

        card.update(from = Bank, to = Dollars, amount = { 100.0 }, exchangeRate = null, resetRate = false)

        card.state.exchangeRate shouldBe 2.0
        card.state.convertedAmount shouldBe 200.0
    }

    private fun transfer(amount: Double, toAmount: Double) = TransactionDraft(
        accountId = Bank.id.value,
        type = TransactionType.TRANSFER,
        amount = amount,
        toAccountId = Dollars.id.value,
        toAmount = toAmount,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Usd = AssetCode.unsafe("USD")
        val Bank = account("Bank", Eur)
        val Dollars = account("Dollars", Usd)

        fun account(name: String, asset: AssetCode) = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = asset,
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )
    }
}
