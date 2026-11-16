package com.nivy.data.repository.mapper

import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCardId
import com.nivy.base.model.IntervalType
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
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
    fun `a planned expense keeps the card it's paid with`() {
        val card = UUID.randomUUID()
        val entity = RecurringEntity.copy(creditCardId = card)

        val rule = with(mapper) { entity.toDomain() }.shouldBeRight()

        rule.creditCard shouldBe CreditCardId(card)
        with(mapper) { rule.toEntity() } shouldBe entity
    }

    @Test
    fun `recurring rule needs an interval`() {
        with(mapper) { RecurringEntity.copy(intervalN = null).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(intervalN = 0).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(intervalType = null).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `rule needs a start date and an amount`() {
        with(mapper) { RecurringEntity.copy(startDate = null).toDomain() }.shouldBeLeft()
        with(mapper) { RecurringEntity.copy(amount = 0.0).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `a planned transfer goes to another account`() {
        val toAccount = UUID.randomUUID()
        val entity = RecurringEntity.copy(type = TransactionType.TRANSFER, toAccountId = toAccount)

        val rule = with(mapper) { entity.toDomain() }.shouldBeRight()

        rule.toAccount shouldBe AccountId(toAccount)
        with(mapper) { rule.toEntity() } shouldBe entity
        with(mapper) { entity.copy(toAccountId = null).toDomain() }.shouldBeLeft()
        with(mapper) { entity.copy(toAccountId = AccountUuid).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `an income or an expense goes to no account`() {
        val entity = RecurringEntity.copy(toAccountId = UUID.randomUUID())

        with(mapper) { entity.toDomain() }.shouldBeRight().toAccount shouldBe null
    }

    @Test
    fun `a recurring rule keeps its end date, a one-time one has none`() {
        val end = Instant.parse("2025-01-15T12:00:00Z")
        val entity = RecurringEntity.copy(endDate = end)

        val rule = with(mapper) { entity.toDomain() }.shouldBeRight()

        rule.endDate shouldBe end
        with(mapper) { rule.toEntity() } shouldBe entity
        with(mapper) { entity.copy(oneTime = true).toDomain() }.shouldBeRight().endDate shouldBe null
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
            toAccount = null,
            amount = PositiveDouble.unsafe(12.5),
            category = CategoryId(CategoryUuid),
            title = NotBlankTrimmedString.unsafe("Music"),
            description = null,
            startDate = Start,
            interval = PlannedPaymentInterval(
                n = PositiveInt.unsafe(1),
                type = IntervalType.MONTH,
            ),
            endDate = null,
            creditCard = null,
        )
    }
}
