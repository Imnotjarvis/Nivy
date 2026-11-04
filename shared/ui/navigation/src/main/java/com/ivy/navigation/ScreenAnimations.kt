package com.ivy.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * The current screen. A screen opening rises a little from below as it fades in, over the one it
 * covers, which stays still, dims and takes no touches meanwhile; going back, it sinks as it fades
 * out, off the one it uncovers. Other changes are instant.
 */
@Composable
internal fun AnimatedScreens(
    navigation: Navigation,
    content: @Composable (Screen?) -> Unit,
) {
    AnimatedContent(
        targetState = navigation.currentScreen,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                // Going back, the screen sinking stays above the one it uncovers.
                targetContentZIndex = if (navigation.animation == ScreenAnimation.Closing) -1f else 0f,
                sizeTransform = null,
            )
        },
        label = "screens",
    ) { screen ->
        val animation = navigation.animation
        val entering = transition.targetState == EnterExitState.Visible
        Box(
            modifier = Modifier.screenAnimation(animation, entering, screenProgress(animation)),
            propagateMinConstraints = true,
        ) {
            content(screen)
        }
    }
}

// From 0 to 1 as a screen enters, from 1 to 0 as it leaves; animated only when [animation] is, and
// part of the screen's transition, so a screen leaving is kept until it's done.
@Composable
private fun AnimatedVisibilityScope.screenProgress(animation: ScreenAnimation?): State<Float> =
    transition.animateFloat(
        transitionSpec = { if (animation == null) snap() else tween(DurationMillis, easing = FastOutSlowInEasing) },
        label = "screen",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }

// How a screen shows during a change: the one on top (opening, or closing) rises into its place as
// it fades in, or sinks as it fades out; the one under it dims as it's covered, and brightens as
// it's uncovered. A screen leaving takes no touches.
private fun Modifier.screenAnimation(
    animation: ScreenAnimation?,
    entering: Boolean,
    progress: State<Float>,
): Modifier {
    val touches = if (entering) this else blockTouches()
    if (animation == null) return touches
    val onTop = (animation == ScreenAnimation.Opening) == entering
    return if (onTop) {
        touches.graphicsLayer {
            translationY = (1f - progress.value) * RiseFraction * size.height
            alpha = progress.value
        }
    } else {
        touches.drawWithContent {
            drawContent()
            drawRect(Color.Black, alpha = DimAlpha * (1f - progress.value))
        }
    }
}

private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

// About a third of a second, starting quickly and settling gently.
private const val DurationMillis = 300

// How far below its place an opening screen starts: a tenth of its height.
private const val RiseFraction = 0.1f

// How dark the screen underneath gets: Material's scrim.
private const val DimAlpha = 0.32f
