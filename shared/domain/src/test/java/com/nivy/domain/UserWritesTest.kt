package com.nivy.domain

import android.database.sqlite.SQLiteFullException
import com.nivy.base.Toaster
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserWritesTest {

    private val toaster = mockk<Toaster>(relaxed = true)
    private val userWrites = UserWrites(toaster)

    @Test
    fun `a write that goes through shows nothing`() = runTest {
        // given
        var written = false

        // when
        val result = userWrites.run(Failed) { written = true }

        // then
        result shouldBe true
        written shouldBe true
        coVerify(exactly = 0) { toaster.show(any<Int>(), any()) }
    }

    @Test
    fun `a write the database refuses shows that it failed`() = runTest {
        // when
        val result = userWrites.run(Failed) { throw SQLiteFullException("database or disk is full") }

        // then
        result shouldBe false
        coVerify { toaster.show(Failed, any()) }
    }

    @Test
    fun `a write cut short by leaving the screen is cancelled, not failed`() = runTest {
        // when
        shouldThrow<CancellationException> {
            userWrites.run(Failed) { throw CancellationException("The screen closed") }
        }

        // then
        coVerify(exactly = 0) { toaster.show(any<Int>(), any()) }
    }

    private companion object {
        const val Failed = 1
    }
}
