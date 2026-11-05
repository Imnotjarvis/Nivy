package com.ivy.data.repository.mapper

import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.model.CreditCardId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class CreditCardMapperTest {
    private val mapper = CreditCardMapper()

    @Test
    fun `maps a card both ways`() {
        with(mapper) {
            Entity.toDomain().shouldBeRight() shouldBe Card
            Card.toEntity() shouldBe Entity
        }
    }

    @Test
    fun `the 31st closes the statement on the month's last day, which null already means`() {
        with(mapper) {
            Entity.copy(statementClosingDay = 31).toDomain().shouldBeRight().statementClosingDay shouldBe null
        }
    }

    @Test
    fun `a card without a name or a charge day isn't valid`() {
        with(mapper) {
            Entity.copy(name = " ").toDomain().shouldBeLeft()
            Entity.copy(chargeDay = 0).toDomain().shouldBeLeft()
            Entity.copy(chargeDay = 32).toDomain().shouldBeLeft()
        }
    }

    @Test
    fun `a limit that isn't positive is none`() {
        with(mapper) {
            Entity.copy(creditLimit = 0.0).toDomain().shouldBeRight().creditLimit shouldBe null
        }
    }

    private companion object {
        val AccountUuid: UUID = UUID.randomUUID()
        val CardUuid: UUID = UUID.randomUUID()

        val Entity = CreditCardEntity(
            accountId = AccountUuid,
            name = "Visa",
            chargeDay = 15,
            statementClosingDay = 25,
            creditLimit = 1500.0,
            id = CardUuid,
        )

        val Card = CreditCard(
            id = CreditCardId(CardUuid),
            account = AccountId(AccountUuid),
            name = NotBlankTrimmedString.unsafe("Visa"),
            chargeDay = 15,
            statementClosingDay = 25,
            creditLimit = PositiveDouble.unsafe(1500.0),
        )
    }
}
