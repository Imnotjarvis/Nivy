package com.nivy.domain.usecase.account

import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.domain.usecase.HomeNumbers
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov1
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov3
import com.nivy.domain.usecase.HomeNumbers.Companion.Nov30
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class AccountTransactionsUseCaseTest {
    private val home = HomeNumbers()

    @Test
    fun `a transfer paid with a card is listed in the receiving account on the day it was made`() = runTest {
        // given a transfer made on November 3 with Bank's card, which charges it on December 15
        val bank = home.account("Bank")
        val cash = home.account("Cash")
        home.transactionDao.save(
            TransactionEntity(
                accountId = bank.id.value,
                type = TransactionType.TRANSFER,
                amount = 120.0,
                toAccountId = cash.id.value,
                toAmount = 120.0,
                dateTime = Dec15,
                purchaseDateTime = Nov3,
            )
        )

        // when
        val november = home.accountTransactions.transactions(cash.id.value, ClosedTimeRange(from = Nov1, to = Nov30))

        // then
        november.single().time shouldBe Nov3
    }

    private companion object {
        val Dec15: Instant = Instant.parse("2026-12-15T12:00:00Z")
    }
}
