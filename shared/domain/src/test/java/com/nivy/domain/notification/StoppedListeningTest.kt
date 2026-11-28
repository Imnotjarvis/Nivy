package com.nivy.domain.notification

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Duration
import java.time.Instant

class StoppedListeningTest {
    @Test
    fun `without apps picked, Nivy isn't expected to listen`() {
        stoppedListening(appsPicked = false, granted = false, heardAt = null, now = Now).shouldBeNull()
    }

    @Test
    fun `with its access off, Nivy stopped`() {
        stoppedListening(appsPicked = true, granted = false, heardAt = Now, now = Now) shouldBe
            Stopped(accessOff = true, since = null)
    }

    @Test
    fun `hearing nothing for more than a day, Nivy stopped since it last heard one`() {
        val heardAt = Now - Duration.ofHours(25)

        stoppedListening(appsPicked = true, granted = true, heardAt = heardAt, now = Now) shouldBe
            Stopped(accessOff = false, since = heardAt)
    }

    @Test
    fun `having heard one today, or none yet since it was set up, Nivy is listening`() {
        stoppedListening(appsPicked = true, granted = true, heardAt = Now - Duration.ofHours(2), now = Now)
            .shouldBeNull()
        stoppedListening(appsPicked = true, granted = true, heardAt = null, now = Now).shouldBeNull()
    }

    private companion object {
        val Now: Instant = Instant.parse("2026-11-28T12:00:00Z")
    }
}
