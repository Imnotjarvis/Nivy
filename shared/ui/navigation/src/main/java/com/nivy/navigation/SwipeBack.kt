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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nivy.design.system.motion.appearTween
import com.nivy.design.utils.thenIf

/**
 * Swiping down to go back. A screen, or a dialog, follows the finger down from where what it shows
 * can't scroll up any further, to the point where letting go goes back, [distancePx] down. There
 * it stops, however far the finger goes, and [onReachPoint] tells so. Let go there and it goes back
 * as Back does, leaving from that point; let go before it, or when Back left it where it is (an
 * open dialog took it), and it slides back into place.
 *
 * @param canGoBack whether there's anything to go back to, asked as a swipe starts.
 * @param goBack goes back: true when it left, false when Back left it where it is.
 */
@Stable
class SwipeBack(
    private val distancePx: Float,
    private val canGoBack: () -> Boolean,
    private val goBack: () -> Boolean,
    private val onReachPoint: () -> Unit,
) : NestedScrollConnection {
    // How far the finger has pulled, past the point too: the screen comes up once it's back above.
    private var pulledPx = 0f

    /** How far down it is, with the finger, up to the point. */
    var offsetPx by mutableFloatStateOf(0f)
        private set

    /** How far it is from going back, from 0 to 1 at the point. */
    val progress: Float get() = offsetPx / distancePx

    /** Drags on what doesn't scroll. */
    val draggable = DraggableState { drag(it) }

    /** The finger moved down by [delta], or up when it's negative, where nothing scrolls. */
    fun drag(delta: Float) {
        if (pulledPx == 0f && (delta <= 0f || !canGoBack())) return
        pull(delta)
    }

    /** The finger let go: at the point it goes back, before it it slides back. */
    suspend fun release() {
        if (pulledPx == 0f) return
        if (offsetPx >= distancePx && goBack()) return
        animate(offsetPx, 0f, animationSpec = appearTween()) { value, _ ->
            offsetPx = value
            pulledPx = value
        }
    }

    private fun pull(delta: Float) {
        val atPoint = offsetPx >= distancePx
        pulledPx = (pulledPx + delta).coerceAtLeast(0f)
        offsetPx = pulledPx.coerceAtMost(distancePx)
        if (!atPoint && offsetPx >= distancePx) onReachPoint()
    }

    // The finger going up while it's down: it comes back up before anything scrolls.
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || pulledPx == 0f) return Offset.Zero
        val up = maxOf(available.y, -pulledPx)
        pull(up)
        return Offset(0f, up)
    }

    // Pulled down past the top of what scrolls, by the finger only: a fling stops at the top.
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        if (pulledPx == 0f && !canGoBack()) return Offset.Zero
        pull(available.y)
        return Offset(0f, available.y)
    }

    // Let go while it's down: the fling is the swipe's, not the list's.
    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pulledPx == 0f) return Velocity.Zero
        release()
        return available
    }
}

/** A [SwipeBack] whose point is 80 dp down, with a light tick when the screen reaches it. */
@Composable
fun rememberSwipeBack(canGoBack: () -> Boolean, goBack: () -> Boolean): SwipeBack {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentCanGoBack by rememberUpdatedState(canGoBack)
    val currentGoBack by rememberUpdatedState(goBack)
    return remember(density, haptics) {
        SwipeBack(
            distancePx = with(density) { SwipeBackDistance.toPx() },
            canGoBack = { currentCanGoBack() },
            goBack = { currentGoBack() },
            onReachPoint = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
        )
    }
}

/**
 * Moves with [swipe], pulled from what scrolls inside it; with [dragAnywhere], dragged from anywhere
 * else on it too.
 */
fun Modifier.swipeBack(swipe: SwipeBack, dragAnywhere: Boolean): Modifier =
    nestedScroll(swipe)
        .thenIf(dragAnywhere) {
            draggable(swipe.draggable, Orientation.Vertical, onDragStopped = { swipe.release() })
        }
        .graphicsLayer { translationY = swipe.offsetPx }

private val SwipeBackDistance = 80.dp
