package com.nivy.data.repository.mapper

import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.QuickAddEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class QuickAddMapperTest {
    private val mapper = QuickAddMapper()

    @Test
    fun `maps a quick add to domain and back`() {
        val quickAdd = with(mapper) { MetroEntity.toDomain() }.shouldBeRight()

        quickAdd shouldBe Metro
        with(mapper) { quickAdd.toEntity() } shouldBe MetroEntity
    }

    @Test
    fun `one without an amount has it entered each time`() {
        val entity = MetroEntity.copy(amount = null)

        val quickAdd = with(mapper) { entity.toDomain() }.shouldBeRight()

        quickAdd.amount shouldBe null
        with(mapper) { quickAdd.toEntity() } shouldBe entity
    }

    @Test
    fun `an amount of nothing or less isn't valid`() {
        with(mapper) { MetroEntity.copy(amount = 0.0).toDomain() }.shouldBeLeft()
        with(mapper) { MetroEntity.copy(amount = -2.2).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `a transfer goes to another account`() {
        val toAccount = UUID.randomUUID()
        val entity = MetroEntity.copy(type = TransactionType.TRANSFER, toAccountId = toAccount)

        val quickAdd = with(mapper) { entity.toDomain() }.shouldBeRight()

        quickAdd.toAccount shouldBe AccountId(toAccount)
        with(mapper) { quickAdd.toEntity() } shouldBe entity
    }

    @Test
    fun `a transfer needs an account to go to, other than its own`() {
        val transfer = MetroEntity.copy(type = TransactionType.TRANSFER)
        with(mapper) { transfer.toDomain() }.shouldBeLeft()
        with(mapper) { transfer.copy(toAccountId = transfer.accountId).toDomain() }.shouldBeLeft()
    }

    @Test
    fun `an expense goes to no other account`() {
        val entity = MetroEntity.copy(toAccountId = UUID.randomUUID())

        val quickAdd = with(mapper) { entity.toDomain() }.shouldBeRight()

        quickAdd.toAccount shouldBe null
    }

    private companion object {
        val Account: UUID = UUID.randomUUID()
        val Transport: UUID = UUID.randomUUID()
        val Visa: UUID = UUID.randomUUID()
        val Id: UUID = UUID.randomUUID()

        val MetroEntity = QuickAddEntity(
            type = TransactionType.EXPENSE,
            accountId = Account,
            amount = 2.2,
            categoryId = Transport,
            title = "Metro",
            creditCardId = Visa,
            toAccountId = null,
            orderNum = 3.0,
            id = Id,
        )

        val Metro = QuickAdd(
            id = QuickAddId(Id),
            type = TransactionType.EXPENSE,
            account = AccountId(Account),
            toAccount = null,
            amount = PositiveDouble.unsafe(2.2),
            category = CategoryId(Transport),
            title = NotBlankTrimmedString.unsafe("Metro"),
            creditCard = CreditCardId(Visa),
            orderNum = 3.0,
        )
    }
}
