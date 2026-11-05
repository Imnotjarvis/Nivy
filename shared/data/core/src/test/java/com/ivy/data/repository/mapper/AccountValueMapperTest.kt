package com.ivy.data.repository.mapper

import com.ivy.data.db.entity.AccountValueEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountValue
import com.ivy.data.model.AccountValueId
import com.ivy.data.model.primitive.NonNegativeDouble
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class AccountValueMapperTest {
    private val mapper = AccountValueMapper()

    @Test
    fun `maps a value both ways`() {
        with(mapper) {
            Entity.toDomain().shouldBeRight() shouldBe Value
            Value.toEntity() shouldBe Entity
        }
    }

    @Test
    fun `a negative value isn't valid`() {
        with(mapper) {
            Entity.copy(value = -1.0).toDomain().shouldBeLeft()
        }
    }

    private companion object {
        val AccountUuid: UUID = UUID.randomUUID()
        val ValueUuid: UUID = UUID.randomUUID()
        val Time: Instant = Instant.parse("2026-10-31T18:00:00Z")

        val Entity = AccountValueEntity(
            accountId = AccountUuid,
            time = Time,
            value = 8_940.0,
            id = ValueUuid,
        )

        val Value = AccountValue(
            id = AccountValueId(ValueUuid),
            account = AccountId(AccountUuid),
            time = Time,
            value = NonNegativeDouble.unsafe(8_940.0),
        )
    }
}
