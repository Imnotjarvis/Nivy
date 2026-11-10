package com.nivy.data.db.dao.read

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.model.TransactionType
import com.nivy.data.db.NivyRoomDatabase
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TagEntity
import com.nivy.data.db.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * The tag queries, and the sweep of the links deleted transactions leave, on the database itself:
 * they answer as FakeTagDao and FakeTagAssociationDao do (FakeDaosMatchSqlTest). Their order isn't
 * asked for, so it isn't checked.
 */
@RunWith(AndroidJUnit4::class)
class TagDaoTest {
    private lateinit var db: NivyRoomDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun aSearchFindsTheTagsWithTheTextAnywhereInTheirNameInAnyCase() = runBlocking {
        listOf("Trip", "summer trips", "Work").forEach { db.writeTagDao.save(tag(it)) }

        assertEquals(setOf("Trip", "summer trips"), db.tagDao.findByText("TRIP").map { it.name }.toSet())
    }

    @Test
    fun theTagsOnWhatsAskedAreFoundThroughTheirLinks() = runBlocking {
        val trip = tag("Trip")
        val work = tag("Work")
        db.writeTagDao.save(trip)
        db.writeTagDao.save(work)
        val lunch = UUID.randomUUID()
        val dinner = UUID.randomUUID()
        link(trip, lunch)
        link(work, lunch)
        link(trip, dinner)

        assertEquals(setOf(trip, work), db.tagDao.findTagsByAssociatedId(lunch).toSet())
        assertEquals(
            mapOf(lunch to setOf(trip, work), dinner to setOf(trip)),
            db.tagDao.findTagsByAssociatedIds(listOf(lunch, dinner)).mapValues { it.value.toSet() },
        )
        assertEquals(emptyMap<UUID, List<TagEntity>>(), db.tagDao.findTagsByAssociatedIds(listOf(UUID.randomUUID())))
    }

    @Test
    fun theSweepDeletesTheLinksToTransactionsThatArentThereAndKeepsTheOthers() = runBlocking {
        val lunch = TransactionEntity(accountId = UUID.randomUUID(), type = TransactionType.EXPENSE, amount = 12.0)
        db.writeTransactionDao.save(lunch)
        val trip = tag("Trip")
        db.writeTagDao.save(trip)
        link(trip, lunch.id)
        link(trip, UUID.randomUUID())

        db.writeTagAssociationDao.deleteLinksOfDeletedTransactions()

        assertEquals(listOf(lunch.id), db.tagAssociationDao.findAll().map { it.associatedId })
    }

    private suspend fun link(tag: TagEntity, to: UUID) {
        db.writeTagAssociationDao.save(TagAssociationEntity(tag.id, to, Instant.EPOCH, isDeleted = false))
    }

    private fun tag(name: String) =
        TagEntity(UUID.randomUUID(), name, null, 0, null, 0.0, Instant.EPOCH, false, Instant.EPOCH)
}
