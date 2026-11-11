package com.nivy.accounts.usecase

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.AccountType
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.account.AccountBalanceDetails
import com.nivy.wallet.data.model.CreditCardUse
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.utils.nivyMaxTime
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AccountDataUseCaseTest {
    private val bank = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Bank"),
        asset = Eur,
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )
    private val visa = card("Visa")
    private val amex = card("Amex")

    private val creditCardRepository = mockk<CreditCardRepository> {
        coEvery { findByAccount(bank.id) } returns listOf(amex, visa)
    }
    private val transactionRepository = mockk<TransactionRepository>()

    private val useCase = AccountDataUseCase(
        exchangeUseCase = mockk(),
        accountBalanceUseCase = mockk {
            coEvery { balance(any(), any()) } returns AccountBalanceDetails(bank, BigDecimal.ZERO)
        },
        accountIncomeExpenseUseCase = mockk {
            coEvery { incomeExpense(any(), any(), any()) } returns IncomeExpensePair.zero()
        },
        creditCardRepository = creditCardRepository,
        transactionRepository = transactionRepository,
        timeProvider = mockk<TimeProvider> { every { utcNow() } returns Now },
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `each card owes the purchases it hasn't charged yet`() = runTest {
        // given: what's dated from now on, as the card charges it, beside a debit expense and income
        coEvery { transactionRepository.findAllByAccountAndBetween(bank.id, Now, nivyMaxTime()) } returns listOf(
            expense(30.0, visa.id),
            expense(12.5, visa.id),
            expense(40.0, amex.id),
            expense(99.0, card = null),
            income(5.0),
        )

        // when
        val data = accountsData().single()

        // then
        data.creditCards shouldBe listOf(CreditCardUse(amex, 40.0), CreditCardUse(visa, 42.5))
    }

    @Test
    fun `an account without cards owes nothing on cards`() = runTest {
        coEvery { creditCardRepository.findByAccount(bank.id) } returns emptyList()

        accountsData().single().creditCards shouldBe emptyList()
    }

    private suspend fun accountsData() = useCase.accountsData(
        accounts = persistentListOf(bank),
        baseCurrency = Eur.code,
        range = ClosedTimeRange(from = Now, to = Now),
    )

    private fun card(name: String) = CreditCard(
        id = CreditCardId(UUID.randomUUID()),
        account = bank.id,
        name = NotBlankTrimmedString.unsafe(name),
        chargeDay = 15,
        statementClosingDay = null,
        creditLimit = null,
    )

    private fun expense(amount: Double, card: CreditCardId?): Transaction = Expense(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse("2026-12-15T12:00:00Z"),
        settled = true,
        metadata = metadata(card),
        value = PositiveValue(PositiveDouble.unsafe(amount), Eur),
        account = bank.id,
    )

    private fun income(amount: Double): Transaction = Income(
        id = TransactionId(UUID.randomUUID()),
        title = null,
        description = null,
        category = null,
        time = Instant.parse("2026-12-15T12:00:00Z"),
        settled = true,
        metadata = metadata(card = null),
        value = PositiveValue(PositiveDouble.unsafe(amount), Eur),
        account = bank.id,
    )

    private fun metadata(card: CreditCardId?) = TransactionMetadata(
        recurringRuleId = null,
        paidForDateTime = null,
        loanRecordId = null,
        purchaseDateTime = null,
        creditCard = card,
    )

    private companion object {
        val Eur = AssetCode.unsafe("EUR")
        val Now: Instant = Instant.parse("2026-11-15T12:00:00Z")
    }
}
