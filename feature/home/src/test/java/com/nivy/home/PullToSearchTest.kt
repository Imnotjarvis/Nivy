package com.nivy.home

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PullToSearchTest {
    private val pull = PullToSearch(slotPx = 100f, flingPx = 50f)

    private fun pullDown(px: Float, source: NestedScrollSource = NestedScrollSource.UserInput) =
        pull.onPostScroll(consumed = Offset.Zero, available = Offset(0f, px), source = source)

    private fun scrollOn(px: Float) = pull.onPreScroll(Offset(0f, -px), NestedScrollSource.UserInput)

    private fun letGo(velocity: Float) = runBlocking(Frames) { pull.onPreFling(Velocity(0f, velocity)) }

    @Test
    fun `pulling down past the list's top shows the bar, at most all of it`() {
        pullDown(30f) shouldBe Offset(0f, 30f)
        pull.shownPx shouldBe 30f

        pullDown(100f) shouldBe Offset(0f, 70f)
        pull.shownPx shouldBe 100f
    }

    @Test
    fun `a fling that reaches the list's top doesn't show it`() {
        pullDown(30f, source = NestedScrollSource.SideEffect) shouldBe Offset.Zero
        pull.shownPx shouldBe 0f
    }

    @Test
    fun `scrolling on takes the bar away before the list moves`() {
        pullDown(80f)

        scrollOn(30f) shouldBe Offset(0f, -30f)
        pull.shownPx shouldBe 50f
        scrollOn(100f) shouldBe Offset(0f, -50f)
        pull.shownPx shouldBe 0f
        scrollOn(10f) shouldBe Offset.Zero
    }

    @Test
    fun `let go slowly part way, the bar settles at the nearer end`() {
        pullDown(60f)
        letGo(velocity = 10f) shouldBe Velocity(0f, 10f)
        pull.shownPx shouldBe 100f

        scrollOn(60f)
        letGo(velocity = -10f)
        pull.shownPx shouldBe 0f
    }

    @Test
    fun `let go with a flick, the bar settles the way the finger was going`() {
        pullDown(30f)
        letGo(velocity = 200f)
        pull.shownPx shouldBe 100f

        scrollOn(30f)
        letGo(velocity = -200f)
        pull.shownPx shouldBe 0f
    }

    @Test
    fun `whole or gone, a fling is the list's`() {
        letGo(velocity = 200f) shouldBe Velocity.Zero

        pullDown(100f)
        letGo(velocity = -200f) shouldBe Velocity.Zero
        pull.shownPx shouldBe 100f
    }

    // Frames 16 ms apart, so that settling runs to its end.
    private object Frames : MonotonicFrameClock {
        private var nanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            nanos += FrameNanos
            return onFrame(nanos)
        }
    }
}

private const val FrameNanos = 16_000_000L
