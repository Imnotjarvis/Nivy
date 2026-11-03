package com.ivy.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
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
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import com.ivy.design.system.shapes.AppCornerRadius

/**
 * Marks a block that opens a screen: the block, as a card of its [color], grows into the screen,
 * like an app out of its icon, and shrinks back into it on the way back. It notes where the block
 * is when it's pressed; nothing while it's only on screen or scrolled.
 */
fun Modifier.opensScreen(color: Color): Modifier = this then OpensScreenElement(color)

private data class OpensScreenElement(val color: Color) : ModifierNodeElement<OpensScreenNode>() {
    override fun create() = OpensScreenNode(color)
    override fun update(node: OpensScreenNode) {
        node.color = color
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "opensScreen"
        properties["color"] = color
    }
}

private class OpensScreenNode(var color: Color) :
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
        currentValueOf(LocalNavigation).pressedBlock = Block(coordinates.boundsInRoot(), color)
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

// Every press forgets the block pressed before; a block that opens a screen notes itself after.
private fun Modifier.forgetPressedBlock(navigation: Navigation) = pointerInput(navigation) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        navigation.pressedBlock = null
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

// How a screen shows during a change: growing from (or shrinking into) the block when it's the
// screen opening (or closing), otherwise as it is. A screen leaving takes no touches.
private fun Modifier.screenAnimation(
    animation: ScreenAnimation?,
    entering: Boolean,
    progress: State<Float>,
): Modifier {
    val touches = if (entering) this else blockTouches()
    val block = animation?.block?.takeIf { animation.opening == entering } ?: return touches
    return touches
        .drawBehind { drawCard(block, progress.value, AppCornerRadius.value.toPx()) }
        .graphicsLayer { showMiniature(block.bounds, progress.value, AppCornerRadius.value.toPx()) }
}

/**
 * Behind the screen, at [progress] 0 to 1: the screen underneath dimming, and over it the card the
 * block becomes, from the block's place to the whole screen, fading in over the block at first.
 */
private fun DrawScope.drawCard(block: Block, progress: Float, blockRadius: Float) {
    if (progress >= 1f) return
    drawRect(Color.Black, alpha = DimAlpha * progress)
    val card = cardBounds(block.bounds, size, progress)
    drawRoundRect(
        color = block.color,
        topLeft = card.topLeft,
        size = card.size,
        cornerRadius = CornerRadius(lerp(blockRadius, 0f, progress)),
        alpha = (progress / CardFadeIn).coerceAtMost(1f),
    )
}

/**
 * The whole screen in miniature inside the card, at [progress] 0 to 1: scaled to fit with a
 * margin, so the card's edges show around it, centered, with corners concentric with the card's,
 * and fading in over the first part.
 */
private fun GraphicsLayerScope.showMiniature(block: Rect, progress: Float, blockRadius: Float) {
    val card = cardBounds(block, size, progress)
    val margin = lerp(MiniatureMargin.toPx(), 0f, progress)
    val scale = minOf(
        (card.width - 2 * margin) / size.width,
        (card.height - 2 * margin) / size.height,
    ).coerceAtLeast(MinScale)
    transformOrigin = TransformOrigin(0f, 0f)
    scaleX = scale
    scaleY = scale
    translationX = card.left + (card.width - size.width * scale) / 2
    translationY = card.top + (card.height - size.height * scale) / 2
    // The corner radius is in the screen's own coordinates, before the scale.
    val radius = (lerp(blockRadius, 0f, progress) - margin).coerceAtLeast(0f)
    shape = RoundedCornerShape(radius / scale)
    clip = true
    alpha = (progress / MiniatureFadeIn).coerceAtMost(1f)
}

// The card at [progress]: from the block's bounds at 0 to the whole screen at 1.
private fun cardBounds(block: Rect, screen: Size, progress: Float) = Rect(
    left = lerp(block.left, 0f, progress),
    top = lerp(block.top, 0f, progress),
    right = lerp(block.right, screen.width, progress),
    bottom = lerp(block.bottom, screen.height, progress),
)

private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

// About a third of a second, like Android's app opening; starting gently, so the block is seen
// growing, and settling gently.
private const val DurationMillis = 350

// How dark the screen underneath gets: Material's scrim.
private const val DimAlpha = 0.32f

// The card fades in over the block, and the miniature into the card, over these parts of the
// animation; going back, they fade out over the same parts.
private const val CardFadeIn = 0.15f
private const val MiniatureFadeIn = 0.35f

// Between the card's edges and the miniature, shrinking to none as the card fills the screen.
private val MiniatureMargin = 8.dp

// A block too small for the margin still gets a (tiny) miniature.
private const val MinScale = 0.01f
