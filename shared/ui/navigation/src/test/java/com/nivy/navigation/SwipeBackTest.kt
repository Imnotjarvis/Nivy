package com.nivy.navigation

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SwipeBackTest {
    private var somewhereToGoBack = true
    private var leavesTheScreen = true
    private var wentBack = 0
    private val swipe = SwipeBack(
        distancePx = 160f,
        flickPx = 1_000f,
        canGoBack = { somewhereToGoBack },
        goBack = {
            wentBack++
            leavesTheScreen
        },
    )

    @Test
    fun `pulled down past the top of what scrolls, the screen follows the finger`() {
        swipe.onPostScroll(Offset.Zero, Offset(0f, 60f), NestedScrollSource.UserInput) shouldBe Offset(0f, 60f)

        swipe.offsetPx shouldBe 60f
    }

    @Test
    fun `dragged down where nothing scrolls, the screen follows the finger`() {
        swipe.drag(60f)

        swipe.offsetPx shouldBe 60f
    }

    @Test
    fun `let go far enough down, it goes back from where it is`() = runTest(Frames) {
        swipe.drag(170f)

        swipe.release(velocity = 0f)

        wentBack shouldBe 1
        swipe.offsetPx shouldBe 170f
    }

    @Test
    fun `let go before that, it slides back into place`() = runTest(Frames) {
        swipe.drag(100f)

        swipe.release(velocity = 0f)

        wentBack shouldBe 0
        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `a flick down goes back before that`() = runTest(Frames) {
        swipe.drag(60f)

        swipe.release(velocity = 1_500f)

        wentBack shouldBe 1
    }

    @Test
    fun `a flick of a few pixels doesn't`() = runTest(Frames) {
        swipe.drag(20f)

        swipe.release(velocity = 3_000f)

        wentBack shouldBe 0
        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `back taken by what's on the screen, a dialog say, the screen slides back into place`() = runTest(Frames) {
        leavesTheScreen = false
        swipe.drag(170f)

        swipe.release(velocity = 0f)

        wentBack shouldBe 1
        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `let go while it's down, the fling is the swipe's, not the list's`() = runTest(Frames) {
        swipe.drag(170f)

        swipe.onPreFling(Velocity(0f, 200f)) shouldBe Velocity(0f, 200f)

        wentBack shouldBe 1
    }

    @Test
    fun `a fling reaching the top of what scrolls doesn't move the screen`() {
        swipe.onPostScroll(Offset.Zero, Offset(0f, 60f), NestedScrollSource.SideEffect) shouldBe Offset.Zero

        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `the finger going up brings the screen up before anything scrolls`() {
        swipe.drag(50f)

        swipe.onPreScroll(Offset(0f, -80f), NestedScrollSource.UserInput) shouldBe Offset(0f, -50f)

        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `with nowhere to go back to, nothing moves`() {
        somewhereToGoBack = false

        swipe.drag(100f)
        swipe.onPostScroll(Offset.Zero, Offset(0f, 60f), NestedScrollSource.UserInput) shouldBe Offset.Zero

        swipe.offsetPx shouldBe 0f
    }

    // Each frame 16 ms after the last: an animation runs to its end at once.
    private object Frames : MonotonicFrameClock {
        private var nanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            nanos += 16_000_000L
            return onFrame(nanos)
        }
    }
}
