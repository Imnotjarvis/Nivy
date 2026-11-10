package com.nivy.data.db

import com.nivy.data.db.dao.fake.FakeAccountDao
import com.nivy.data.db.entity.AccountEntity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountsWithoutCurrencyTest {
    @Test
    fun `accounts stored without a currency get the one given, and the others keep theirs`() = runTest {
        // given: one with none, one with a blank one, one in dollars
        val accounts = FakeAccountDao()
        accounts.save(AccountEntity(name = "Cash", color = 0, currency = null))
        accounts.save(AccountEntity(name = "Old", color = 0, currency = ""))
        accounts.save(AccountEntity(name = "Dollars", color = 0, currency = "USD"))

        // when
        val given = giveAccountsWithoutCurrency("EUR", accounts, accounts)

        // then
        given shouldBe 2
        accounts.findAll().associate { it.name to it.currency } shouldBe
            mapOf("Cash" to "EUR", "Old" to "EUR", "Dollars" to "USD")
    }
}
