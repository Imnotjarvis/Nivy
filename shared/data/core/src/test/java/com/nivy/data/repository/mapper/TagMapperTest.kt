package com.nivy.data.repository.mapper

import com.nivy.data.db.entity.TagEntity
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TagMapperTest {
    private val mapper = TagMapper()

    @Test
    fun `a stored tag is its id, its name and when it was made`() {
        // given: a row as an old backup may hold it, with the columns nothing reads filled in
        val entity = ValidEntity.copy(description = "from a backup", color = 7, icon = "trip", orderNum = 3.0)

        // when
        val res = with(mapper) { entity.toDomain() }

        // then
        res.shouldBeRight() shouldBe ValidTag
    }

    @Test
    fun `a tag is stored with the columns nothing reads as every tag has them`() {
        // when
        val entity = with(mapper) { ValidTag.toEntity() }

        // then
        entity shouldBe ValidEntity
    }

    @Test
    fun `a stored tag without a name is refused`() {
        // when
        val res = with(mapper) { ValidEntity.copy(name = " ").toDomain() }

        // then
        res.shouldBeLeft()
    }

    @Test
    fun `a new tag takes the id it's given, and its name`() {
        // given
        val id = TagId(UUID.randomUUID())

        // when
        val tag = mapper.createNewTag(tagId = id, name = NotBlankTrimmedString.unsafe("Trip"))

        // then
        tag.id shouldBe id
        tag.name.value shouldBe "Trip"
    }

    private companion object {
        val Id: UUID = UUID.randomUUID()
        val Made: Instant = Instant.parse("2026-11-03T12:00:00Z")

        val ValidEntity = TagEntity(
            id = Id,
            name = "Trip",
            description = null,
            color = 0,
            icon = null,
            orderNum = 0.0,
            dateTime = Made,
            isDeleted = false,
            lastSyncedTime = Instant.EPOCH,
        )
        val ValidTag = Tag(
            id = TagId(Id),
            name = NotBlankTrimmedString.unsafe("Trip"),
            creationTimestamp = Made,
        )
    }
}
