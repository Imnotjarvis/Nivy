package com.ivy.data.repository

import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.DeleteOperation
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class RepositoryMemoTest {
    @JvmInline
    private value class ItemId(override val value: UUID) : UniqueId

    private data class Item(override val id: ItemId) : Identifiable<ItemId>

    @Test
    fun `after AllDataChange the memo reads the database again`() = runTest {
        // given a memo that has read everything once
        val dataObserver = DataObserver()
        val memo = fakeRepositoryMemoFactory(dataObserver).createMemo<Item, ItemId>(
            getDataWriteSaveEvent = { DataWriteEvent.AllDataChange },
            getDateWriteDeleteEvent = { DataWriteEvent.AllDataChange },
        )
        var reads = 0
        val readAll: suspend () -> List<Item> = {
            reads++
            listOf(Item(ItemId(UUID.randomUUID())))
        }
        memo.findAll(readAll) { toList() }
        memo.findAll(readAll) { toList() }
        reads shouldBe 1

        // when the tables are rewritten underneath it
        dataObserver.post(DataWriteEvent.AllDataChange)

        // then the next read goes to the database
        memo.findAll(readAll) { toList() }
        reads shouldBe 2
        memo.findAllMemoized shouldBe true
    }

    @Test
    fun `a row deleted while findAll is reading the table does not come back`() = runTest {
        // given a read of the whole table that is still in progress
        val memo = fakeRepositoryMemoFactory().createMemo<Item, ItemId>(
            getDataWriteSaveEvent = { DataWriteEvent.SaveTags(emptyList()) },
            getDateWriteDeleteEvent = { DataWriteEvent.DeleteTags(DeleteOperation.All) },
        )
        val kept = Item(ItemId(UUID.randomUUID()))
        val deleted = Item(ItemId(UUID.randomUUID()))
        val readStarted = CompletableDeferred<Unit>()
        val readFinishes = CompletableDeferred<Unit>()
        val reading = launch {
            memo.findAll(
                findAllOperation = {
                    readStarted.complete(Unit)
                    readFinishes.await()
                    listOf(kept, deleted)
                },
                sortMemo = { toList() },
            )
        }
        readStarted.await()

        // when a row is deleted before that read finishes
        val deleting = launch { memo.deleteById(deleted.id) { } }
        readFinishes.complete(Unit)
        reading.join()
        deleting.join()

        // then the memo answers without the deleted row
        memo.items.keys.toSet() shouldBe setOf(kept.id)
        memo.findAll(
            findAllOperation = { error("the memo should answer this") },
            sortMemo = { toList() },
        ) shouldBe listOf(kept)
    }
}
