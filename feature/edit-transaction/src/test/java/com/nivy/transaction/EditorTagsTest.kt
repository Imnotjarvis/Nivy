package com.nivy.transaction

import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.AssociationId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.transaction.EditTransactionViewEvent.TagEvent
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
    private var tagsAdded = 0

    private fun TestScope.editorTags() = EditorTags(
        transactionTags = transactionTags,
        scope = this,
        transactionId = { transaction },
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
        tags.copyTo(copy)

        // then
        coVerify(exactly = 1) { transactionTags.associateTag(AssociationId(copy), holiday) }
        coVerify(exactly = 0) { transactionTags.associateTag(AssociationId(transaction), any()) }
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
