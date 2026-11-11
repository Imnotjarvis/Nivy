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
    private var ticks = 0
    private val swipe = SwipeBack(
        distancePx = 160f,
        canGoBack = { somewhereToGoBack },
        goBack = {
            wentBack++
            leavesTheScreen
        },
        onReachPoint = { ticks++ },
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
    fun `the screen stops at the point however far the finger goes, with one tick`() {
        swipe.drag(100f)
        swipe.drag(150f)
        swipe.onPostScroll(Offset.Zero, Offset(0f, 50f), NestedScrollSource.UserInput)

        swipe.offsetPx shouldBe 160f
        ticks shouldBe 1
    }

    @Test
    fun `past the point, the screen comes up once the finger is back above it`() {
        swipe.drag(300f)

        swipe.drag(-100f)
        swipe.offsetPx shouldBe 160f

        swipe.drag(-60f)
        swipe.offsetPx shouldBe 140f
    }

    @Test
    fun `back above the point and down to it again, it ticks again`() {
        swipe.drag(170f)
        swipe.drag(-30f)
        swipe.drag(30f)

        ticks shouldBe 2
    }

    @Test
    fun `let go at the point, it goes back from there`() = runTest(Frames) {
        swipe.drag(250f)

        swipe.release()

        wentBack shouldBe 1
        swipe.offsetPx shouldBe 160f
    }

    @Test
    fun `let go before it, it slides back into place`() = runTest(Frames) {
        swipe.drag(100f)

        swipe.release()

        wentBack shouldBe 0
        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `a flick before the point doesn't go back`() = runTest(Frames) {
        swipe.drag(100f)

        swipe.onPreFling(Velocity(0f, 5_000f))

        wentBack shouldBe 0
        swipe.offsetPx shouldBe 0f
    }

    @Test
    fun `back taken by what's on the screen, a dialog say, the screen slides back into place`() = runTest(Frames) {
        leavesTheScreen = false
        swipe.drag(170f)

        swipe.release()

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
