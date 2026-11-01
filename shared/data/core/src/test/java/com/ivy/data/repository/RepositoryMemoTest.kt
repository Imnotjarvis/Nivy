package com.ivy.data.repository

import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import io.kotest.matchers.shouldBe
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
}
