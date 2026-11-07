package com.nivy.domain.usecase.planned

import arrow.core.None
import arrow.core.Some
import com.nivy.base.model.AccountType
import com.nivy.base.model.IntervalType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import com.nivy.data.repository.AccountRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PlannedPaymentAmountsUseCaseTest {
    private val accountRepository = mockk<AccountRepository>()
    private val exchangeUseCase = mockk<ExchangeUseCase>()
    private val useCase = PlannedPaymentAmountsUseCase(accountRepository, exchangeUseCase)

    private val dollars = account(Usd)
    private val euros = account(Eur)
    private val levs = account(Bgn)

    init {
        coEvery { accountRepository.findById(any()) } returns null
        listOf(dollars, euros, levs).forEach { account ->
            coEvery { accountRepository.findById(account.id) } returns account
        }
        coEvery { exchangeUseCase.convert(any<PositiveValue>(), Usd) } answers {
            val value = firstArg<PositiveValue>()
            if (value.asset == Usd) Some(value) else None
        }
        coEvery { exchangeUseCase.convert(value(10.0, Eur), Usd) } returns Some(value(11.0, Usd))
    }

    @Test
    fun `the total converts each rule's amount`() = runTest {
        // when
        val total = useCase.total(listOf(rule(10.0, euros.id), rule(5.0, dollars.id)), Usd)

        // then
        total shouldBe 16.0
    }

    @Test
    fun `without a rate the amount is left out`() = runTest {
        // when
        val total = useCase.total(listOf(rule(7.0, levs.id), rule(5.0, dollars.id)), Usd)

        // then
        total shouldBe 5.0
    }

    @Test
    fun `without a rate a recurring rule is left out of the monthly total too`() = runTest {
        // when
        val rules = listOf(
            rule(7.0, levs.id, every = IntervalType.MONTH),
            rule(30.0, dollars.id, every = IntervalType.MONTH),
        )
        val total = useCase.monthlyTotal(rules, Usd)

        // then
        total shouldBe 30.0
    }

    @Test
    fun `without its account the amount is taken to be in the currency asked`() = runTest {
        // when
        val total = useCase.total(listOf(rule(7.0, AccountId(UUID.randomUUID()))), Usd)

        // then
        total shouldBe 7.0
    }

    private fun value(amount: Double, asset: AssetCode) =
        PositiveValue(PositiveDouble.unsafe(amount), asset)

    private fun account(asset: AssetCode) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(asset.code),
        asset = asset,
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )

    private fun rule(amount: Double, account: AccountId, every: IntervalType? = null) = PlannedPaymentRule(
        id = PlannedPaymentRuleId(UUID.randomUUID()),
        type = TransactionType.EXPENSE,
        account = account,
        amount = PositiveDouble.unsafe(amount),
        category = null,
        title = null,
        description = null,
        startDate = Start,
        interval = every?.let { PlannedPaymentInterval(n = PositiveInt.unsafe(1), type = it) },
        creditCard = null,
    )

    private companion object {
        val Usd = AssetCode.unsafe("USD")
        val Eur = AssetCode.unsafe("EUR")
        val Bgn = AssetCode.unsafe("BGN")
        val Start: Instant = Instant.parse("2026-11-01T09:00:00Z")
    }
}
