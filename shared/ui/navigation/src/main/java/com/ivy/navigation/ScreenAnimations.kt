package com.ivy.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import com.ivy.design.system.shapes.AppCornerRadius

/**
 * Marks a block that opens a screen: the screen grows out of the block, like an app out of its
 * icon, and shrinks back into it on the way back. It notes where the block is when it's pressed;
 * nothing while it's only on screen or scrolled.
 */
fun Modifier.opensScreen(): Modifier = this then OpensScreenElement

private data object OpensScreenElement : ModifierNodeElement<OpensScreenNode>() {
    override fun create() = OpensScreenNode()
    override fun update(node: OpensScreenNode) = Unit
}

private class OpensScreenNode :
    Modifier.Node(),
    LayoutAwareModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {
    private var coordinates: LayoutCoordinates? = null

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    // The press reaches the screens (which forget the block pressed before) before the block.
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial || pointerEvent.type != PointerEventType.Press) return
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return
        currentValueOf(LocalNavigation).pressedBlock = coordinates.boundsInRoot()
    }

    override fun onCancelPointerInput() = Unit
}

/**
 * The current screen. A screen opened from a block grows out of it over the one it covers, which
 * stays still and takes no touches meanwhile; going back, it shrinks into the block over the one
 * it uncovers. Other changes are instant.
 */
@Composable
internal fun AnimatedScreens(
    navigation: Navigation,
    content: @Composable (Screen?) -> Unit,
) {
    AnimatedContent(
        targetState = navigation.currentScreen,
        modifier = Modifier.forgetPressedBlock(navigation),
        transitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                // Going back, the screen shrinking stays above the one it uncovers.
                targetContentZIndex = if (navigation.animation?.opening == false) -1f else 0f,
                sizeTransform = null,
            )
        },
        label = "screens",
    ) { screen ->
        Box(
            modifier = screenAnimation(navigation.animation),
            propagateMinConstraints = true,
        ) {
            content(screen)
        }
    }
}

// Every press forgets the block pressed before; a block that opens a screen notes itself after.
private fun Modifier.forgetPressedBlock(navigation: Navigation) = pointerInput(navigation) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        navigation.pressedBlock = null
    }
}

// How a screen shows during a change: growing from (or shrinking into) the block when it's the
// screen opening (or closing), otherwise as it is, kept until the other one is done.
@Composable
private fun AnimatedVisibilityScope.screenAnimation(animation: ScreenAnimation?): Modifier {
    val progress = transition.animateFloat(
        transitionSpec = { if (animation == null) snap() else tween(DurationMillis, easing = Emphasized) },
        label = "screen",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    val entering = transition.targetState == EnterExitState.Visible
    val leaving = if (entering) Modifier else Modifier.blockTouches()
    val block = animation?.block?.takeIf { animation.opening == entering } ?: return leaving
    return leaving.graphicsLayer {
        growFrom(block, progress.value, AppCornerRadius.value.toPx())
    }
}

/**
 * Draws the screen as a window over [block] at [progress] 0, growing to the full screen at 1: the
 * screen scaled to the window's width, cut to its height with the block's rounded corners, and
 * fading in over the first part.
 */
private fun GraphicsLayerScope.growFrom(block: Rect, progress: Float, blockRadius: Float) {
    val width = lerp(block.width, size.width, progress)
    val height = lerp(block.height, size.height, progress)
    val scale = width / size.width
    transformOrigin = TransformOrigin(0f, 0f)
    scaleX = scale
    scaleY = scale
    translationX = lerp(block.left, 0f, progress)
    translationY = lerp(block.top, 0f, progress)
    // The clip is in the screen's own coordinates, before the scale.
    shape = Window(height = height / scale, radius = lerp(blockRadius, 0f, progress) / scale)
    clip = true
    alpha = (progress / FadeInPart).coerceAtMost(1f)
}

// The top of the screen, [height] tall, with rounded corners.
private data class Window(val height: Float, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(0f, 0f, size.width, height, CornerRadius(radius)))
}

private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

// Like Android's app opening: about a third of a second, fast at first and settling gently.
private const val DurationMillis = 350
private val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

// The screen fades in over this part of the animation, and out over the same part going back.
private const val FadeInPart = 0.3f
