package com.nivy.data.repository

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.write.WriteTagAssociationDao
import com.nivy.data.db.dao.write.WriteTagDao
import com.nivy.data.model.TagId
import com.nivy.data.repository.fake.fakeRepositoryMemoFactory
import com.nivy.data.repository.mapper.TagMapper
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
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

    @Test
    fun `deleting a tag deletes its links and the tag in one database transaction`() = runTest {
        // when
        repository.deleteById(TagId(UUID.randomUUID()))

        // then
        writes shouldBe listOf(true, true)
    }
}
