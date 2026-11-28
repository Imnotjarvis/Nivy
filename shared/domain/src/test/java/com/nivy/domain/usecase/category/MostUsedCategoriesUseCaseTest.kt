package com.nivy.domain.usecase.category

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.TransactionType
import com.nivy.base.time.impl.FixedTimeProvider
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
import java.time.LocalDateTime
import java.util.UUID

class MostUsedCategoriesUseCaseTest {
    private val groceries = category("Groceries")
    private val coffee = category("Coffee")
    private val salary = category("Salary")

    private val dao = FakeTransactionDao()
    private val useCase = MostUsedCategoriesUseCase(
        transactionDao = dao,
        categoryRepository = mockk<CategoryRepository> {
            coEvery { findAll() } returns listOf(coffee, groceries, salary)
        },
        timeProvider = November24,
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `a type's categories used most over the last 12 months come the most first`() = runTest {
        // given: today is November 24, 2026
        dao.saveMany(
            listOf(
                transaction(coffee, "2026-11-20T08:00:00Z"),
                transaction(groceries, "2026-11-01T10:00:00Z"),
                transaction(groceries, "2026-06-01T10:00:00Z"),
                // 12 months and a day ago: not counted.
                transaction(coffee, "2025-11-23T08:00:00Z"),
                transaction(coffee, "2025-11-23T09:00:00Z"),
                transaction(salary, "2026-10-27T09:00:00Z", TransactionType.INCOME),
            )
        )

        // then
        useCase.mostUsed(TransactionType.EXPENSE) shouldBe listOf(groceries, coffee)
        useCase.mostUsed(TransactionType.INCOME) shouldBe listOf(salary)
    }

    @Test
    fun `a category deleted since isn't one to choose`() = runTest {
        // given
        val deleted = UUID.randomUUID()
        dao.saveMany(
            listOf(
                transaction(groceries, "2026-11-01T10:00:00Z"),
                TransactionEntity(
                    accountId = Account,
                    type = TransactionType.EXPENSE,
                    amount = 1.0,
                    dateTime = Instant.parse("2026-11-02T10:00:00Z"),
                    categoryId = deleted,
                ),
            )
        )

        // then
        useCase.mostUsed(TransactionType.EXPENSE) shouldBe listOf(groceries)
    }

    private fun transaction(
        category: Category,
        dateTime: String,
        type: TransactionType = TransactionType.EXPENSE,
    ) = TransactionEntity(
        accountId = Account,
        type = type,
        amount = 1.0,
        dateTime = Instant.parse(dateTime),
        categoryId = category.id.value,
    )

    private fun category(name: String) = Category(
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(0),
        icon = null,
        id = CategoryId(UUID.nameUUIDFromBytes(name.toByteArray())),
        orderNum = 0.0,
    )

    private companion object {
        val Account: UUID = UUID.randomUUID()

        /** November 24, 2026, at noon, in UTC. */
        val November24 = FixedTimeProvider(LocalDateTime.of(2026, 11, 24, 12, 0))
    }
}
