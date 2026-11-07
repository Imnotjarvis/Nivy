package com.nivy.wallet.migrations.impl

import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.fake.FakeAccountDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.TransactionEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class KeepTransfersWithDeletedAccountsMigrationTest {

    private val accountDao = FakeAccountDao()
    private val transactionDao = FakeTransactionDao(accounts = accountDao)
    private val migration = KeepTransfersWithDeletedAccountsMigration(accountDao, transactionDao, transactionDao)

    private val revolut = AccountEntity(name = "Revolut", currency = "USD", color = 0)
    private val cash = AccountEntity(name = "Cash", currency = "USD", color = 0)
    private val deleted = UUID.randomUUID()

    private suspend fun balanceOf(account: AccountEntity): Double =
        transactionDao.balance(account.id, Instant.EPOCH, Instant.parse("2100-01-01T00:00:00Z"))

    @Test
    fun `a transfer to a deleted account becomes the paying account's expense, which its balance counts again`() =
        runTest {
            // given
            accountDao.saveMany(listOf(revolut, cash))
            val savings = transfer(
                from = revolut.id,
                sent = 60.0,
                to = deleted,
                received = 50.0,
                description = "Savings",
            )
            transactionDao.save(savings)
            balanceOf(revolut) shouldBe 0.0

            // when
            migration.migrate()

            // then
            transactionDao.findAll().single() shouldBe savings.copy(
                type = TransactionType.EXPENSE,
                toAccountId = null,
                toAmount = null,
                description = "Savings\nTransfer to a deleted account",
            )
            balanceOf(revolut) shouldBe -60.0
        }

    @Test
    fun `a transfer from a deleted account becomes the receiving account's income, at what it received`() = runTest {
        // given
        accountDao.saveMany(listOf(revolut))
        val refill = transfer(from = deleted, sent = 20.0, to = revolut.id, received = 24.0)
        transactionDao.save(refill)
        balanceOf(revolut) shouldBe 0.0

        // when
        migration.migrate()

        // then
        transactionDao.findAll().single() shouldBe refill.copy(
            type = TransactionType.INCOME,
            accountId = revolut.id,
            amount = 24.0,
            toAccountId = null,
            toAmount = null,
            description = "Transfer from a deleted account",
        )
        balanceOf(revolut) shouldBe 24.0
    }

    @Test
    fun `everything else stays as it is`() = runTest {
        // given
        accountDao.saveMany(listOf(revolut, cash))
        val betweenAccounts = transfer(from = revolut.id, sent = 10.0, to = cash.id, received = 10.0)
        val betweenDeletedOnes = transfer(from = deleted, sent = 5.0, to = UUID.randomUUID(), received = 5.0)
        val expense = TransactionEntity(
            accountId = revolut.id,
            type = TransactionType.EXPENSE,
            amount = 3.0,
            dateTime = Instant.parse("2026-11-04T10:00:00Z"),
        )
        val all = listOf(betweenAccounts, betweenDeletedOnes, expense)
        transactionDao.saveMany(all)

        // when
        migration.migrate()

        // then
        transactionDao.findAll() shouldContainExactlyInAnyOrder all
    }

    private fun transfer(
        from: UUID,
        sent: Double,
        to: UUID,
        received: Double,
        description: String? = null,
    ) = TransactionEntity(
        accountId = from,
        type = TransactionType.TRANSFER,
        amount = sent,
        toAccountId = to,
        toAmount = received,
        description = description,
        dateTime = Instant.parse("2026-11-03T10:00:00Z"),
    )
}
