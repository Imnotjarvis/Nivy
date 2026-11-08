package com.nivy.wallet.domain.logic

import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.TransactionEntity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class SmartTitleSuggestionsLogicTest {
    private val dao = FakeTransactionDao()
    private val logic = SmartTitleSuggestionsLogic(dao)
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
        suggestions.toList() shouldBe listOf("Groceries", "Gym", "Gas")
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
        suggestions.toList() shouldBe listOf("Lunch", "Lunch At Work")
    }
}
