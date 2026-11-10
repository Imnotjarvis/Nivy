package com.nivy.categories

import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.wallet.domain.data.SortOrder
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class CategoryListTest {
    private val food = data("Food", order = 2, balance = -120.0, expenses = 120.0)
    private val transport = data("Transport", order = 0, balance = -45.5, expenses = 45.5)
    private val groceries = data("Groceries", order = 1, balance = 0.4, expenses = 0.0)
    private val salary = data("Salary", order = 3, balance = 2_000.0, expenses = 0.0)
    private val categories = listOf(food, transport, groceries, salary)

    @Test
    fun `a search lists the categories whose name holds it, whatever the case`() {
        categories.matching("foo") shouldBe listOf(food)
        categories.matching(" GRO ") shouldBe listOf(groceries)
    }

    @Test
    fun `a search matches the names only, not the text around them`() {
        categories.matching("a") shouldBe listOf(transport, salary)
        categories.matching("value") shouldBe emptyList()
    }

    @Test
    fun `a blank search lists every category`() {
        categories.matching("") shouldBe categories
        categories.matching("  ") shouldBe categories
    }

    @Test
    fun `the default order is the one the person chose`() {
        categories.inOrder(SortOrder.DEFAULT) shouldBe listOf(transport, groceries, food, salary)
    }

    @Test
    fun `by balance, the highest first, and those without a balance last`() {
        categories.inOrder(SortOrder.BALANCE_AMOUNT) shouldBe listOf(salary, transport, food, groceries)
    }

    @Test
    fun `by expenses, the most spent first, and alphabetically by name`() {
        categories.inOrder(SortOrder.EXPENSES) shouldBe listOf(food, transport, groceries, salary)
        categories.inOrder(SortOrder.ALPHABETICAL) shouldBe listOf(food, groceries, salary, transport)
    }

    private fun data(name: String, order: Int, balance: Double, expenses: Double) = CategoryData(
        category = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(0),
            icon = null,
            orderNum = order.toDouble(),
        ),
        monthlyBalance = balance,
        monthlyExpenses = expenses,
        monthlyIncome = balance + expenses,
    )
}
