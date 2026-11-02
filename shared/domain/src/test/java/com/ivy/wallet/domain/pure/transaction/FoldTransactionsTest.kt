package com.ivy.wallet.domain.pure.transaction

import arrow.core.nonEmptyListOf
import com.ivy.data.model.Transaction
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

class FoldTransactionsTest {
    @Test
    fun `each value function sums over every transaction`() = runTest {
        val transactions = List(10_000) { mockk<Transaction>() }

        val sums = foldTransactionsSuspend(
            transactions = transactions,
            valueFunctions = nonEmptyListOf(
                { _, _: Unit -> BigDecimal.ONE },
                { _, _: Unit -> BigDecimal("0.5") },
            ),
            arg = Unit,
        )

        sums shouldBe nonEmptyListOf(BigDecimal("10000"), BigDecimal("5000.0"))
    }
}
