package com.nivy.domain.usecase.wallet

import com.nivy.base.TestDispatchersProvider
import com.nivy.domain.usecase.HomeNumbers
import com.nivy.domain.usecase.HomeNumbers.Companion.Euros
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov1
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov30
import com.nivy.domain.usecase.HomeNumbers.Companion.Oct20
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Home's balance and its month's income and expenses, from the real use cases. */
class WalletNumbersTest {
    private val home = HomeNumbers()
    private val balance = WalletBalanceUseCase(
        accountRepository = home.accounts,
        accountBalanceUseCase = home.accountBalance,
        exchangeUseCase = home.exchange,
        dispatchers = TestDispatchersProvider,
    )
    private val incomeExpense = WalletIncomeExpenseUseCase(
        accountTransactionsUseCase = home.accountTransactions,
        exchangeUseCase = home.exchange,
        dispatchers = TestDispatchersProvider,
    )
    private val november = ClosedTimeRange(from = Nov1, to = Nov30)

    @Test
    fun `the balance adds up the accounts in the balance, in the base currency`() = runTest {
        val bank = home.account("Bank")
        val dollars = home.account("Dollars", currency = "USD")
        val savings = home.account("Savings", included = false)
        home.rate("USD", perEuro = 1.1)
        home.income(bank, 1_000.0)
        home.expense(bank, 40.0)
        home.income(dollars, 110.0)
        home.income(savings, 500.0)

        // 960 euros, and 110 dollars as 100 euros; the savings aren't in the balance.
        balance.balance(Euros).toDouble() shouldBe (1_060.0 plusOrMinus Cent)
    }

    @Test
    fun `an account in a currency without a rate counts as nothing`() = runTest {
        val bank = home.account("Bank")
        val pounds = home.account("Pounds", currency = "GBP")
        home.income(bank, 100.0)
        home.income(pounds, 50.0)

        balance.balance(Euros).toDouble() shouldBe (100.0 plusOrMinus Cent)
    }

    @Test
    fun `the month's income and expenses, of the accounts in the balance, in the base currency`() = runTest {
        val bank = home.account("Bank")
        val dollars = home.account("Dollars", currency = "USD")
        val savings = home.account("Savings", included = false)
        home.rate("USD", perEuro = 1.1)
        home.income(bank, 1_000.0)
        home.expense(bank, 40.0)
        home.income(dollars, 110.0)
        home.expense(dollars, 22.0)
        home.income(savings, 500.0)
        // October's, out of the month.
        home.expense(bank, 75.0, on = Oct20)

        val month = incomeExpense.incomeExpense(Euros, home.accounts.findAll(), november)

        month.income.toDouble() shouldBe (1_100.0 plusOrMinus Cent)
        month.expense.toDouble() shouldBe (60.0 plusOrMinus Cent)
    }

    private companion object {
        const val Cent = 0.005
    }
}
