package com.nivy.data.repository

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.db.dao.fake.FakeCreditCardDao
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.mapper.CreditCardMapper
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class CreditCardRepositoryTest {

    private val dao = FakeCreditCardDao()
    private val repository = CreditCardRepository(
        creditCardDao = dao,
        writeCreditCardDao = dao,
        mapper = CreditCardMapper(),
        dispatchersProvider = TestDispatchersProvider,
    )
    private val bank = AccountId(UUID.randomUUID())
    private val other = AccountId(UUID.randomUUID())

    @Test
    fun `saving an account's cards deletes the ones it no longer has, and leaves other accounts' cards`() =
        runTest {
            // given
            val visa = card(bank, "Visa")
            val amex = card(bank, "Amex")
            val othersCard = card(other, "Other")
            repository.saveAccountCards(bank, listOf(visa, amex))
            repository.saveAccountCards(other, listOf(othersCard))

            // when
            val gold = visa.copy(name = NotBlankTrimmedString.unsafe("Visa Gold"))
            val mastercard = card(bank, "Mastercard")
            repository.saveAccountCards(bank, listOf(gold, mastercard))

            // then
            repository.findByAccount(bank) shouldContainExactlyInAnyOrder listOf(gold, mastercard)
            repository.findByAccount(other) shouldBe listOf(othersCard)
        }

    private fun card(account: AccountId, name: String) = CreditCard(
        id = CreditCardId(UUID.randomUUID()),
        account = account,
        name = NotBlankTrimmedString.unsafe(name),
        chargeDay = 10,
        statementClosingDay = null,
        creditLimit = null,
    )
}
