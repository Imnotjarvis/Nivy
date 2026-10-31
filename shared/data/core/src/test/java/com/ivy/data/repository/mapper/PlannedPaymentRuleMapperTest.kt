package com.ivy.data.repository.mapper

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.CategoryId
import com.ivy.base.model.IntervalType
import com.ivy.data.model.PlannedPaymentInterval
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.PositiveInt
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PlannedPaymentRuleMapperTest {
    private val mapper = PlannedPaymentRuleMapper()

    @Test
    fun `maps recurring rule to domain and back`() {
        // when
        val rule = with(mapper) { RecurringEntity.toDomain() }.shouldBeRight()

        // then
        rule shouldBe RecurringRule
        with(mapper) { rule.toEntity() } shouldBe RecurringEntity
    }

    @Test
    fun `one-time rule has no interval`() {
        // given
        val entity = RecurringEntity.copy(oneTime = true)

        // when
        val rule = with(mapper) { entity.toDomain() }.shouldBeRight()

        // then
        rule.interval shouldBe null
        with(mapper) { rule.toEntity() } shouldBe entity.copy(intervalN = null, intervalType = null)
    }

    @Test
    fun `recurring rule needs an interval`() {
        with(mapper) { RecurringEntity.copy(intervalN = null).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(intervalN = 0).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(intervalType = null).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `rule needs a start date, an amount and not be a transfer`() {
        with(mapper) { RecurringEntity.copy(startDate = null).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(amount = 0.0).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(type = TransactionType.TRANSFER).toDomain() }
            .shouldBeLeft()
    }

    companion object {
        val RuleUuid: UUID = UUID.randomUUID()
        val AccountUuid: UUID = UUID.randomUUID()
        val CategoryUuid: UUID = UUID.randomUUID()
        val Start: Instant = Instant.parse("2024-01-15T09:00:00Z")

        val RecurringEntity = PlannedPaymentRuleEntity(
            startDate = Start,
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            oneTime = false,
            type = TransactionType.EXPENSE,
            accountId = AccountUuid,
            amount = 12.5,
            categoryId = CategoryUuid,
            title = "Music",
            description = null,
            id = RuleUuid,
        )

        val RecurringRule = PlannedPaymentRule(
            id = PlannedPaymentRuleId(RuleUuid),
            type = TransactionType.EXPENSE,
            account = AccountId(AccountUuid),
            amount = PositiveDouble.unsafe(12.5),
            category = CategoryId(CategoryUuid),
            title = NotBlankTrimmedString.unsafe("Music"),
            description = null,
            startDate = Start,
            interval = PlannedPaymentInterval(
                n = PositiveInt.unsafe(1),
                type = IntervalType.MONTH,
            ),
        )
    }
}
