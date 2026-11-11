package com.nivy.domain.usecase.category

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.domain.usecase.HomeNumbers
import com.nivy.domain.usecase.HomeNumbers.Companion.Euros
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

/** A category's income and expenses, as the categories and the pie chart show them. */
class CategoryIncomeExpenseUseCaseTest {
    private val home = HomeNumbers()
    private val useCase = CategoryIncomeExpenseUseCase(
        transactionsIncomeExpenseUseCase = TransactionsIncomeExpenseUseCase(home.exchange, TestDispatchersProvider),
        dispatchers = TestDispatchersProvider,
    )
    private val food = category("Food")
    private val rent = category("Rent")

    @Test
    fun `a category's expenses, in the base currency, of the accounts asked`() = runTest {
        val bank = home.account("Bank")
        val dollars = home.account("Dollars", currency = "USD")
        val card = home.account("Card")
        home.rate("USD", perEuro = 1.1)
        home.expense(bank, 40.0, category = food.id.value)
        home.expense(dollars, 22.0, category = food.id.value)
        home.expense(bank, 800.0, category = rent.id.value)
        home.expense(card, 15.0, category = food.id.value)

        val food = useCase.incomeExpense(
            transactions = home.transactions.findAll(),
            accountFilterList = listOf(bank, dollars),
            category = food,
            baseCurrency = Euros,
        )

        // 40 euros and 22 dollars, as 20 euros; not the rent, nor the card's.
        food.expense.toDouble() shouldBe (60.0 plusOrMinus Cent)
        food.income.toDouble() shouldBe (0.0 plusOrMinus Cent)
    }

    @Test
    fun `the transactions without a category, for the Unspecified slice`() = runTest {
        val bank = home.account("Bank")
        home.expense(bank, 12.0)
        home.expense(bank, 40.0, category = food.id.value)

        val unspecified = useCase.incomeExpense(
            transactions = home.transactions.findAll(),
            accountFilterList = listOf(bank),
            category = null,
            baseCurrency = Euros,
        )

        unspecified.expense.toDouble() shouldBe (12.0 plusOrMinus Cent)
    }

    @Test
    fun `an expense in a currency without a rate counts as nothing`() = runTest {
        val bank = home.account("Bank")
        val pounds = home.account("Pounds", currency = "GBP")
        home.expense(bank, 40.0, category = food.id.value)
        home.expense(pounds, 25.0, category = food.id.value)

        val food = useCase.incomeExpense(
            transactions = home.transactions.findAll(),
            accountFilterList = listOf(bank, pounds),
            category = food,
            baseCurrency = Euros,
        )

        food.expense.toDouble() shouldBe (40.0 plusOrMinus Cent)
    }

    private fun category(name: String) = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(0),
        icon = null,
        orderNum = 0.0,
    )

    private companion object {
        const val Cent = 0.005
    }
}
