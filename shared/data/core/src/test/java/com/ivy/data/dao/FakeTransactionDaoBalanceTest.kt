package com.ivy.data.dao

import com.ivy.data.db.dao.BalanceCases
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** FakeTransactionDao's copy of the balance passes [BalanceCases], as the database's query does. */
class FakeTransactionDaoBalanceTest {
    @Test
    fun `each account's transactions add up as when read one by one`() = runTest {
        // given
        val accounts = FakeAccountDao().apply { saveMany(BalanceCases.accounts) }
        val dao = FakeTransactionDao(accounts).apply { saveMany(BalanceCases.transactions) }

        // then
        BalanceCases.cases.forEach {
            withClue(it.description) {
                dao.balance(it.account, it.start, it.end) shouldBe it.balance.plusOrMinus(BalanceCases.Cents)
            }
        }
    }
}
