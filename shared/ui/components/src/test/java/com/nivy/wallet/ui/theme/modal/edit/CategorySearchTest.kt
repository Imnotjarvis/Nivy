package com.nivy.wallet.ui.theme.modal.edit

import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class CategorySearchTest {
    private val food = category("Food & Drinks")
    private val orderFood = category("Order food")
    private val selfDevelopment = category("Self-development")
    private val groceries = category("Groceries")
    private val all = listOf(groceries, orderFood, selfDevelopment, food)

    @Test
    fun `categories go from A to Z, whatever the case or accents`() {
        listOf(groceries, category("bills"), category("Èlite"), food, category("Arte")).aToZ().names() shouldBe
            listOf("Arte", "bills", "Èlite", "Food & Drinks", "Groceries")
    }

    @Test
    fun `a category is found by the start of its name or of a word in it, whatever the case`() {
        all.matching("FO").names() shouldBe listOf("Food & Drinks", "Order food")
        all.matching("dev").names() shouldBe listOf("Self-development")
        all.matching("ood").shouldBeEmpty()
    }

    @Test
    fun `the names that start with what's typed come first`() {
        listOf(orderFood, food).matching("fo").names() shouldBe listOf("Food & Drinks", "Order food")
    }

    @Test
    fun `a blank search finds them all`() {
        all.matching("  ") shouldBe all
    }

    @Test
    fun `what's typed is shown where the name or the word it starts begins`() {
        matchStart("Food & Drinks", "fo") shouldBe 0
        matchStart("Order food", "fo") shouldBe 6
        matchStart("Self-development", "Dev") shouldBe 5
        matchStart("Groceries", "ce") shouldBe null
        matchStart("Groceries", " ") shouldBe null
    }

    private fun List<Category>.names() = map { it.name.value }

    private fun category(name: String) = Category(
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(0),
        icon = null,
        id = CategoryId(UUID.nameUUIDFromBytes(name.toByteArray())),
        orderNum = 0.0,
    )
}
