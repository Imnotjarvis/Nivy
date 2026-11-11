package com.nivy.domain.usecase.account

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.domain.usecase.HomeNumbers
import com.nivy.domain.usecase.HomeNumbers.Companion.Today
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** An account's income and expenses, and adjusting its balance, from the real use cases. */
class AccountNumbersTest {
    private val home = HomeNumbers()
    private val incomeExpense = AccountIncomeExpenseUseCase(
        accountTransactionsUseCase = home.accountTransactions,
        timeProvider = Today,
        dispatchers = TestDispatchersProvider,
    )
    private val adjust = AdjustAccountBalanceUseCase(
        accountBalanceUseCase = home.accountBalance,
        transactionRepository = home.transactions,
        timeProvider = Today,
    )

    @Test
    fun `an account's income and expenses leave its transfers out, unless they're asked for`() = runTest {
        val bank = home.account("Bank")
        val cash = home.account("Cash")
        home.income(bank, 1_000.0)
        home.expense(bank, 40.0)
        home.transfer(bank, cash, 300.0)
        home.transfer(cash, bank, 50.0)

        val without = incomeExpense.incomeExpense(bank)
        without.income.toDouble() shouldBe (1_000.0 plusOrMinus Cent)
        without.expense.toDouble() shouldBe (40.0 plusOrMinus Cent)

        val with = incomeExpense.incomeExpense(bank, includeTransfersInCalc = true)
        with.income.toDouble() shouldBe (1_050.0 plusOrMinus Cent)
        with.expense.toDouble() shouldBe (340.0 plusOrMinus Cent)
    }

    @Test
    fun `raising a balance adds an income of the difference`() = runTest {
        val bank = home.account("Bank")
        home.income(bank, 100.0)

        adjust.adjust(bank, newBalance = 150.0)

        val adjustment = home.transactions.findAll().single { it.title?.value == "Adjust balance" }
        adjustment.shouldBeInstanceOf<Income>().value.amount.value shouldBe (50.0 plusOrMinus Cent)
        home.accountBalance.balance(bank).balance.toDouble() shouldBe (150.0 plusOrMinus Cent)
    }

    @Test
    fun `lowering a balance adds an expense of the difference`() = runTest {
        val bank = home.account("Bank")
        home.income(bank, 100.0)

        adjust.adjust(bank, newBalance = 70.0)

        val adjustment = home.transactions.findAll().single { it.title?.value == "Adjust balance" }
        adjustment.shouldBeInstanceOf<Expense>().value.amount.value shouldBe (30.0 plusOrMinus Cent)
        home.accountBalance.balance(bank).balance.toDouble() shouldBe (70.0 plusOrMinus Cent)
    }

    @Test
    fun `the same balance adds nothing`() = runTest {
        val bank = home.account("Bank")
        home.income(bank, 100.0)

        adjust.adjust(bank, newBalance = 100.0)

        home.transactions.findAll().size shouldBe 1
    }

    private companion object {
        const val Cent = 0.005
    }
}
