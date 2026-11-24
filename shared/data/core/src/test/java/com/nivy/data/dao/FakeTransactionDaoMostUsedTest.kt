package com.nivy.data.dao

import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.MostUsedCategoryCases
import com.nivy.data.db.dao.fake.FakeTransactionDao
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** FakeTransactionDao's categories used most pass [MostUsedCategoryCases], as the database's query does. */
class FakeTransactionDaoMostUsedTest {
    @Test
    fun `a type's categories used most since a time come the most first, then the one used last`() = runTest {
        // given
        val dao = FakeTransactionDao().apply { saveMany(MostUsedCategoryCases.transactions) }

        // when
        val mostUsed = dao.findMostUsedCategoryIds(
            type = TransactionType.EXPENSE,
            since = MostUsedCategoryCases.Since,
            limit = MostUsedCategoryCases.Limit,
        )

        // then
        mostUsed shouldBe MostUsedCategoryCases.MostUsedExpenses
    }
}
