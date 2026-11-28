package com.nivy.wallet.domain.logic

import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.CategoryRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class SmartTitleSuggestionsLogicTest {
    private val groceries = category("Groceries")
    private val restaurant = category("Restaurant")
    private val orderFood = category("Order food")
    private val dao = FakeTransactionDao()
    private val logic = SmartTitleSuggestionsLogic(
        transactionDao = dao,
        categoryRepository = mockk<CategoryRepository> {
            coEvery { findAll() } returns listOf(groceries, restaurant, orderFood)
        },
    )
    private val food = UUID.randomUUID()

    @Test
    fun `a category's titles come most used first`() = runTest {
        // given
        listOf("groceries", "Gas", "Groceries ", "Gym", "gym", "Groceries").forEach { title ->
            dao.save(
                TransactionEntity(
                    accountId = UUID.randomUUID(),
                    type = TransactionType.EXPENSE,
                    amount = 1.0,
                    title = title,
                    categoryId = food,
                )
            )
        }

        // when
        val suggestions = logic.suggest(title = null, categoryId = food, accountId = null)

        // then
        suggestions.map { it.title } shouldBe listOf("Groceries", "Gym", "Gas")
    }

    @Test
    fun `titles that start with what's typed are suggested, in any case`() = runTest {
        // given
        listOf("Lunch", "lunch at work", "Brunch", "Lunch").forEach { title ->
            dao.save(
                TransactionEntity(
                    accountId = UUID.randomUUID(),
                    type = TransactionType.EXPENSE,
                    amount = 1.0,
                    title = title,
                )
            )
        }

        // when
        val suggestions = logic.suggest(title = "lun", categoryId = null, accountId = null)

        // then
        suggestions.map { it.title } shouldBe listOf("Lunch", "Lunch At Work")
    }

    @Test
    fun `each title brings the category it's in most, and of those it's in as often, the one it was in last`() =
        runTest {
            // given: on one account
            val bank = UUID.randomUUID()
            listOf(
                Triple("Supermarket", groceries, "2026-11-01T10:00:00Z"),
                Triple("supermarket", groceries, "2026-10-01T10:00:00Z"),
                Triple("Supermarket", restaurant, "2026-11-20T10:00:00Z"),
                Triple("Pizza", restaurant, "2026-10-01T20:00:00Z"),
                Triple("Pizza", orderFood, "2026-11-01T20:00:00Z"),
                Triple("Bar Roma", null, "2026-11-02T08:00:00Z"),
            ).forEach { (title, category, dateTime) ->
                dao.save(
                    TransactionEntity(
                        accountId = bank,
                        type = TransactionType.EXPENSE,
                        amount = 1.0,
                        title = title,
                        dateTime = Instant.parse(dateTime),
                        categoryId = category?.id?.value,
                    )
                )
            }

            // when
            val suggestions = logic.suggest(title = null, categoryId = null, accountId = bank)

            // then
            suggestions shouldBe listOf(
                TitleSuggestion("Supermarket", groceries),
                TitleSuggestion("Pizza", orderFood),
                TitleSuggestion("Bar Roma", null),
            )
        }

    @Test
    fun `a title's category is the one its transactions are in most, whatever its case`() = runTest {
        // given: two in groceries, one in a restaurant, and another shop
        listOf(
            "CONAD SUPERSTORE" to groceries,
            "Conad Superstore" to groceries,
            "conad superstore" to restaurant,
            "Conad City" to restaurant,
        ).forEach { (title, category) ->
            dao.save(
                TransactionEntity(
                    accountId = UUID.randomUUID(),
                    type = TransactionType.EXPENSE,
                    amount = 1.0,
                    title = title,
                    categoryId = category.id.value,
                    dateTime = Instant.EPOCH,
                )
            )
        }

        // then
        logic.categoryOf("CONAD SUPERSTORE") shouldBe groceries
        logic.categoryOf("Bar Roma") shouldBe null
    }

    private fun category(name: String) = Category(
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(0),
        icon = null,
        id = CategoryId(UUID.nameUUIDFromBytes(name.toByteArray())),
        orderNum = 0.0,
    )
}
