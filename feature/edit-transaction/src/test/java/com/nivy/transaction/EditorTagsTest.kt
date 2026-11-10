package com.nivy.transaction

import android.database.sqlite.SQLiteFullException
import com.nivy.base.Toaster
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.domain.UserWrites
import com.nivy.transaction.EditTransactionViewEvent.TagEvent
import com.nivy.ui.R
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EditorTagsTest {

    private val transaction = UUID.randomUUID()
    private val holiday = tag("Holiday")
    private val work = tag("Work")
    private val transactionTags = mockk<TransactionTags>()
    private val toaster = mockk<Toaster>(relaxed = true)
    private var tagsAdded = 0

    private fun TestScope.editorTags(stored: Boolean = true) = EditorTags(
        transactionTags = transactionTags,
        userWrites = UserWrites(toaster),
        scope = this,
        transactionId = { transaction },
        stored = { stored },
        afterTagAdd = { tagsAdded++ },
        io = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `the dialog lists all tags, the transaction's own among them`() = runTest {
        // given
        coEvery { transactionTags.associatedTagIds(AssociationId(transaction)) } returns persistentListOf(holiday.id)
        val tags = editorTags()

        // when
        tags.show(all = persistentListOf(holiday, work), transaction = transaction)

        // then
        tags.all shouldBe persistentListOf(holiday, work)
        tags.associated shouldBe persistentListOf(holiday.id)
    }

    @Test
    fun `a tag the database refuses shows that it failed, and the list stays`() = runTest {
        // given
        coEvery { transactionTags.associatedTagIds(AssociationId(transaction)) } returns persistentListOf()
        coEvery { transactionTags.saveTag("trip") } throws SQLiteFullException("database or disk is full")
        val tags = editorTags()
        tags.show(all = persistentListOf(holiday, work), transaction = transaction)

        // when
        tags.onEvent(TagEvent.SaveTag("trip"))
        advanceUntilIdle()

        // then
        tags.all shouldBe persistentListOf(holiday, work)
        tagsAdded shouldBe 0
        coVerify { toaster.show(R.string.msg_change_not_saved, any()) }
    }

    @Test
    fun `selecting a tag links it to the transaction`() = runTest {
        // given
        coEvery { transactionTags.associateTag(AssociationId(transaction), work) } returns persistentListOf(work.id)
        val tags = editorTags()

        // when
        tags.onEvent(TagEvent.OnTagSelect(work))
        advanceUntilIdle()

        // then
        tags.associated shouldBe persistentListOf(work.id)
    }

    @Test
    fun `unselecting a tag unlinks it from the transaction`() = runTest {
        // given
        coEvery { transactionTags.associatedTagIds(AssociationId(transaction)) } returns persistentListOf(holiday.id)
        coEvery { transactionTags.removeTagAssociation(AssociationId(transaction), holiday) } returns persistentListOf()
        val tags = editorTags()
        tags.show(all = persistentListOf(holiday, work), transaction = transaction)

        // when
        tags.onEvent(TagEvent.OnTagDeSelect(holiday))
        advanceUntilIdle()

        // then
        tags.associated shouldBe persistentListOf()
    }

    @Test
    fun `a search typed quickly runs once, for what was typed last`() = runTest {
        // given
        coEvery { transactionTags.searchTags("w") } returns persistentListOf(holiday, work)
        coEvery { transactionTags.searchTags("wo") } returns persistentListOf(work)
        val tags = editorTags()

        // when "w", then "wo" before the search waits out
        tags.onEvent(TagEvent.OnTagSearch("w"))
        advanceTimeBy(100)
        tags.onEvent(TagEvent.OnTagSearch("wo"))
        advanceUntilIdle()

        // then
        coVerify(exactly = 0) { transactionTags.searchTags("w") }
        coVerify(exactly = 1) { transactionTags.searchTags("wo") }
        tags.all shouldBe persistentListOf(work)
    }

    @Test
    fun `an added tag is saved and listed, then what follows an added tag runs`() = runTest {
        // given
        coEvery { transactionTags.saveTag("Work") } returns persistentListOf(holiday, work)
        val tags = editorTags()

        // when
        tags.onEvent(TagEvent.SaveTag("Work"))
        advanceUntilIdle()

        // then
        tags.all shouldBe persistentListOf(holiday, work)
        tagsAdded shouldBe 1
    }

    @Test
    fun `an edited or deleted tag lists the tags as they are then`() = runTest {
        // given
        val office = work.copy(name = NotBlankTrimmedString.unsafe("Office"))
        coEvery { transactionTags.updateTag(office) } returns persistentListOf(holiday, office)
        coEvery { transactionTags.deleteTag(holiday) } returns persistentListOf(office)
        val tags = editorTags()

        // when
        tags.onEvent(TagEvent.OnTagEdit(oldTag = work, newTag = office))
        advanceUntilIdle()

        // then
        tags.all shouldBe persistentListOf(holiday, office)

        // when
        tags.onEvent(TagEvent.OnTagDelete(holiday))
        advanceUntilIdle()

        // then
        tags.all shouldBe persistentListOf(office)
    }

    @Test
    fun `a search lists the tags it matches, half a second after it's typed`() = runTest {
        // given
        coEvery { transactionTags.searchTags("ho") } returns persistentListOf(holiday)
        val tags = editorTags()

        // when
        tags.onEvent(TagEvent.OnTagSearch("ho"))
        advanceTimeBy(499)
        runCurrent()

        // then
        coVerify(exactly = 0) { transactionTags.searchTags(any()) }

        // when
        advanceTimeBy(2)
        runCurrent()

        // then
        tags.all shouldBe persistentListOf(holiday)
    }

    @Test
    fun `a copy of the transaction has its tags once the copying is done`() = runTest {
        // given
        val copy = UUID.randomUUID()
        coEvery { transactionTags.associatedTagIds(AssociationId(transaction)) } returns persistentListOf(holiday.id)
        coEvery { transactionTags.findByIds(listOf(holiday.id)) } returns listOf(holiday)
        coEvery { transactionTags.associateTag(AssociationId(copy), holiday) } returns persistentListOf(holiday.id)
        val tags = editorTags()
        tags.show(all = persistentListOf(holiday, work), transaction = transaction)

        // when
        tags.linkTo(copy)

        // then
        coVerify(exactly = 1) { transactionTags.associateTag(AssociationId(copy), holiday) }
        coVerify(exactly = 0) { transactionTags.associateTag(AssociationId(transaction), any()) }
    }

    @Test
    fun `a new transaction's tags are kept until it's saved, then linked to it`() = runTest {
        // given
        coEvery { transactionTags.associatedTagIds(AssociationId(transaction)) } returns persistentListOf()
        coEvery { transactionTags.associateTag(any(), any()) } returns persistentListOf()
        coEvery { transactionTags.removeTagAssociation(any(), any()) } returns persistentListOf()
        coEvery { transactionTags.findByIds(listOf(work.id)) } returns listOf(work)
        val tags = editorTags(stored = false)
        tags.show(all = persistentListOf(holiday, work), transaction = transaction)

        // when
        tags.onEvent(TagEvent.OnTagSelect(holiday))
        tags.onEvent(TagEvent.OnTagSelect(work))
        tags.onEvent(TagEvent.OnTagDeSelect(holiday))
        advanceUntilIdle()

        // then
        tags.associated shouldBe persistentListOf(work.id)
        coVerify(exactly = 0) { transactionTags.associateTag(any(), any()) }
        coVerify(exactly = 0) { transactionTags.removeTagAssociation(any(), any()) }

        // when
        tags.linkTo(transaction)

        // then
        coVerify(exactly = 1) { transactionTags.associateTag(AssociationId(transaction), work) }
        coVerify(exactly = 0) { transactionTags.associateTag(any(), holiday) }
    }

    @Test
    fun `a deleted tag is no longer one of the transaction's`() = runTest {
        // given
        coEvery {
            transactionTags.associatedTagIds(AssociationId(transaction))
        } returns persistentListOf(holiday.id, work.id)
        coEvery { transactionTags.deleteTag(holiday) } returns persistentListOf(work)
        val tags = editorTags()
        tags.show(all = persistentListOf(holiday, work), transaction = transaction)

        // when
        tags.onEvent(TagEvent.OnTagDelete(holiday))
        advanceUntilIdle()

        // then
        tags.all shouldBe persistentListOf(work)
        tags.associated shouldBe persistentListOf(work.id)
    }
}

private fun tag(name: String) = Tag(
    id = TagId(UUID.nameUUIDFromBytes(name.toByteArray())),
    name = NotBlankTrimmedString.unsafe(name),
    description = null,
    color = ColorInt(0),
    icon = null,
    orderNum = 0.0,
    creationTimestamp = Instant.EPOCH,
)
