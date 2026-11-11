package com.nivy.data.repository

import com.nivy.base.TestDispatchersProvider
import com.nivy.base.model.TransactionType
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.fake.FakeTagAssociationDao
import com.nivy.data.db.dao.fake.FakeTagDao
import com.nivy.data.db.dao.fake.FakeTransactionDao
import com.nivy.data.db.dao.write.WriteTagAssociationDao
import com.nivy.data.db.dao.write.WriteTagDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.fake.fakeRepositoryMemoFactory
import com.nivy.data.repository.mapper.TagMapper
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TagRepositoryTest {
    // The database transaction's block runs as it is; writes says, for each write in turn,
    // whether it was made within one.
    private var inTransaction = false
    private val writes = mutableListOf<Boolean>()
    private val databaseTransaction = object : DatabaseTransaction(mockk(), mockk()) {
        override suspend fun <R> run(block: suspend () -> R): R {
            inTransaction = true
            return try {
                block()
            } finally {
                inTransaction = false
            }
        }
    }
    private val writeTagDao = mockk<WriteTagDao> {
        coEvery { deleteById(any()) } answers { writes += inTransaction }
    }
    private val writeTagAssociationDao = mockk<WriteTagAssociationDao> {
        coEvery { deleteAssociationsByTagId(any()) } answers { writes += inTransaction }
    }

    private val repository = TagRepository(
        mapper = TagMapper(),
        tagDao = mockk(),
        tagAssociationDao = mockk(),
        writeTagDao = writeTagDao,
        writeTagAssociationDao = writeTagAssociationDao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
        databaseTransaction = databaseTransaction,
    )

    // The same repository over in-memory DAOs, which answer as the database's queries do.
    private val transactionDao = FakeTransactionDao()
    private val linkDao = FakeTagAssociationDao(transactions = transactionDao)
    private val tagDao = FakeTagDao(associations = linkDao)
    private val tags = TagRepository(
        mapper = TagMapper(),
        tagDao = tagDao,
        tagAssociationDao = linkDao,
        writeTagDao = tagDao,
        writeTagAssociationDao = linkDao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
        databaseTransaction = databaseTransaction,
    )

    @Test
    fun `deleting a tag deletes its links and the tag in one database transaction`() = runTest {
        // when
        repository.deleteById(TagId(UUID.randomUUID()))

        // then
        writes shouldBe listOf(true, true)
    }

    @Test
    fun `a transaction's tags are found through its links, for one transaction or many`() = runTest {
        // given
        val trip = tag("Trip")
        val work = tag("Work")
        tags.save(trip)
        tags.save(work)
        val lunch = AssociationId(UUID.randomUUID())
        val dinner = AssociationId(UUID.randomUUID())

        // when
        tags.associateTagToEntity(lunch, trip.id)
        tags.associateTagToEntity(lunch, work.id)
        tags.associateTagToEntity(dinner, trip.id)

        // then
        tags.findByAssociatedId(lunch) shouldBe listOf(trip, work)
        tags.findByAssociatedId(listOf(lunch, dinner)) shouldBe
            mapOf(lunch to listOf(trip, work), dinner to listOf(trip))
    }

    @Test
    fun `a tag taken off a transaction is no longer one of its tags`() = runTest {
        // given
        val trip = tag("Trip")
        tags.save(trip)
        val lunch = AssociationId(UUID.randomUUID())
        tags.associateTagToEntity(lunch, trip.id)

        // when
        tags.removeTagAssociation(lunch, trip.id)

        // then
        tags.findByAssociatedId(lunch) shouldBe emptyList()
    }

    @Test
    fun `a search finds the tags with what's typed anywhere in their name, in any case`() = runTest {
        // given
        listOf("Trip", "summer trips", "Work").forEach { tags.save(tag(it)) }

        // when
        val found = tags.findByText("TRIP")

        // then
        found.map { it.name.value } shouldBe listOf("Trip", "summer trips")
    }

    @Test
    fun `a deleted tag is on no transaction any more`() = runTest {
        // given
        val trip = tag("Trip")
        tags.save(trip)
        val lunch = AssociationId(UUID.randomUUID())
        tags.associateTagToEntity(lunch, trip.id)

        // when
        tags.deleteById(trip.id)

        // then
        tags.findByAssociatedId(lunch) shouldBe emptyList()
        linkDao.findAll() shouldBe emptyList()
    }

    @Test
    fun `the links to transactions that aren't there are deleted, and the others kept`() = runTest {
        // given
        val trip = tag("Trip")
        tags.save(trip)
        val lunch = TransactionEntity(accountId = UUID.randomUUID(), type = TransactionType.EXPENSE, amount = 12.0)
        transactionDao.save(lunch)
        tags.associateTagToEntity(AssociationId(lunch.id), trip.id)
        tags.associateTagToEntity(AssociationId(UUID.randomUUID()), trip.id)

        // when
        tags.deleteLinksOfDeletedTransactions()

        // then
        linkDao.findAll().map { it.associatedId } shouldBe listOf(lunch.id)
    }
}

private fun tag(name: String) = Tag(
    id = TagId(UUID.randomUUID()),
    name = NotBlankTrimmedString.unsafe(name),
    creationTimestamp = Instant.EPOCH,
)
