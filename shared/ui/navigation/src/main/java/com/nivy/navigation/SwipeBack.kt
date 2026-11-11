package com.nivy.navigation

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nivy.design.system.motion.appearTween
import com.nivy.design.utils.thenIf

/**
 * Swiping down to go back. A screen, or a dialog, follows the finger down from where what it shows
 * can't scroll up any further. Let go once it's [distancePx] down, or with a flick down, and it goes
 * back as Back does; otherwise, or when Back left it where it is (an open dialog took it), it slides
 * back into place.
 *
 * @param canGoBack whether there's anything to go back to, asked as a swipe starts.
 * @param goBack goes back: true when it left, false when Back left it where it is.
 */
@Stable
class SwipeBack(
    private val distancePx: Float,
    private val flickPx: Float,
    private val canGoBack: () -> Boolean,
    private val goBack: () -> Boolean,
) : NestedScrollConnection {
    /** How far down it is, with the finger. */
    var offsetPx by mutableFloatStateOf(0f)
        private set

    /** How far it is from going back, from 0 to 1 once it's far enough down to. */
    val progress: Float get() = (offsetPx / distancePx).coerceAtMost(1f)

    /** Drags on what doesn't scroll. */
    val draggable = DraggableState { drag(it) }

    /** The finger moved down by [delta], or up when it's negative, where nothing scrolls. */
    fun drag(delta: Float) {
        if (offsetPx == 0f && (delta <= 0f || !canGoBack())) return
        offsetPx = (offsetPx + delta).coerceAtLeast(0f)
    }

    /** The finger let go, moving down at [velocity]: a flick goes back from a quarter of the way. */
    suspend fun release(velocity: Float) {
        if (offsetPx == 0f) return
        val back = offsetPx >= distancePx || velocity >= flickPx && offsetPx >= distancePx / FlickShare
        if (back && goBack()) return
        animate(offsetPx, 0f, animationSpec = appearTween()) { value, _ -> offsetPx = value }
    }

    // The finger going up while it's down: it comes back up before anything scrolls.
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || offsetPx == 0f) return Offset.Zero
        val up = maxOf(available.y, -offsetPx)
        offsetPx += up
        return Offset(0f, up)
    }

    // Pulled down past the top of what scrolls, by the finger only: a fling stops at the top.
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        if (offsetPx == 0f && !canGoBack()) return Offset.Zero
        offsetPx += available.y
        return Offset(0f, available.y)
    }

    // Let go while it's down: the fling is the swipe's, not the list's.
    override suspend fun onPreFling(available: Velocity): Velocity {
        if (offsetPx == 0f) return Velocity.Zero
        release(available.y)
        return available
    }
}

/** A [SwipeBack] that goes back once 160 dp down, or on a flick down. */
@Composable
fun rememberSwipeBack(canGoBack: () -> Boolean, goBack: () -> Boolean): SwipeBack {
    val density = LocalDensity.current
    val currentCanGoBack by rememberUpdatedState(canGoBack)
    val currentGoBack by rememberUpdatedState(goBack)
    return remember(density) {
        with(density) {
            SwipeBack(
                distancePx = SwipeBackDistance.toPx(),
                flickPx = SwipeBackFlick.toPx(),
                canGoBack = { currentCanGoBack() },
                goBack = { currentGoBack() },
            )
        }
    }
}

/**
 * Moves with [swipe], pulled from what scrolls inside it; with [dragAnywhere], dragged from anywhere
 * else on it too.
 */
fun Modifier.swipeBack(swipe: SwipeBack, dragAnywhere: Boolean): Modifier =
    nestedScroll(swipe)
        .thenIf(dragAnywhere) {
            draggable(swipe.draggable, Orientation.Vertical, onDragStopped = { swipe.release(it) })
        }
        .graphicsLayer { translationY = swipe.offsetPx }

private val SwipeBackDistance = 160.dp

// A second's worth: a flick moves this far in one.
private val SwipeBackFlick = 1_000.dp

private const val FlickShare = 4
