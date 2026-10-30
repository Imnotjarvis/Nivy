package com.ivy.data.repository.mapper

import com.ivy.data.db.entity.BudgetEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.Budget
import com.ivy.data.model.BudgetId
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class BudgetMapperTest {
    private val mapper = BudgetMapper()

    @Test
    fun `maps entity to domain`() {
        // when
        val res = with(mapper) { ValidEntity.toDomain() }

        // then
        res.shouldBeRight() shouldBe ValidBudget
    }

    @Test
    fun `maps domain to entity and back`() {
        // when
        val entity = with(mapper) { ValidBudget.toEntity() }

        // then
        entity.categoryIdsSerialized shouldBe "$Category1,$Category2"
        entity.accountIdsSerialized shouldBe Account1.toString()
        with(mapper) { entity.toDomain() }.shouldBeRight() shouldBe ValidBudget
    }

    @Test
    fun `no ids mean every category and account`() {
        // given
        val entity = ValidEntity.copy(categoryIdsSerialized = null, accountIdsSerialized = "")

        // when
        val res = with(mapper) { entity.toDomain() }.shouldBeRight()

        // then
        res.categories shouldBe emptyList()
        res.accounts shouldBe emptyList()
        with(mapper) { res.toEntity() }.categoryIdsSerialized shouldBe ""
    }

    @Test
    fun `ids that aren't valid are skipped`() {
        // given
        val entity = ValidEntity.copy(categoryIdsSerialized = "nope,$Category1")

        // when
        val res = with(mapper) { entity.toDomain() }.shouldBeRight()

        // then
        res.categories shouldBe listOf(CategoryId(Category1))
    }

    @Test
    fun `blank name is not valid`() {
        with(mapper) { ValidEntity.copy(name = " ").toDomain() }.shouldBeLeft()
    }

    @Test
    fun `zero amount is not valid`() {
        with(mapper) { ValidEntity.copy(amount = 0.0).toDomain() }.shouldBeLeft()
    }

    companion object {
        val Id: UUID = UUID.randomUUID()
        val Category1: UUID = UUID.randomUUID()
        val Category2: UUID = UUID.randomUUID()
        val Account1: UUID = UUID.randomUUID()

        val ValidEntity = BudgetEntity(
            name = "Food",
            amount = 300.0,
            categoryIdsSerialized = "$Category1,$Category2",
            accountIdsSerialized = "$Account1",
            orderId = 2.0,
            id = Id,
        )

        val ValidBudget = Budget(
            id = BudgetId(Id),
            name = NotBlankTrimmedString.unsafe("Food"),
            amount = PositiveDouble.unsafe(300.0),
            categories = listOf(CategoryId(Category1), CategoryId(Category2)),
            accounts = listOf(AccountId(Account1)),
            orderNum = 2.0,
        )
    }
}
