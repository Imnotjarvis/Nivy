package com.nivy.wallet.migrations

import com.nivy.base.model.TransactionType
import com.nivy.data.db.dao.fake.FakeTagAssociationDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.wallet.migrations.impl.TagLinksOfDeletedTransactionsMigration
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TagLinksOfDeletedTransactionsMigrationTest {
    @Test
    fun `the tag links deleted transactions left behind are deleted, and the others kept`() = runTest {
        // given
        val transactions = FakeTransactionDao()
        val links = FakeTagAssociationDao(transactions = transactions)
        val lunch = TransactionEntity(accountId = UUID.randomUUID(), type = TransactionType.EXPENSE, amount = 12.0)
        transactions.save(lunch)
        val trip = UUID.randomUUID()
        val toLunch = TagAssociationEntity(trip, lunch.id, Instant.EPOCH, isDeleted = false)
        links.save(toLunch)
        links.save(TagAssociationEntity(trip, UUID.randomUUID(), Instant.EPOCH, isDeleted = false))

        // when
        TagLinksOfDeletedTransactionsMigration(links).migrate()

        // then
        links.findAll() shouldBe listOf(toLunch)
    }
}
