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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
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
        .graphicsLayer { showInCard(block.bounds, progress.value, AppCornerRadius.value.toPx()) }
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
 * The screen, at [progress] 0 to 1: at its own size and place, seen only inside the card, and only
 * at the end, fading in once the card nearly fills the screen; until then the card is a plain color.
 */
private fun GraphicsLayerScope.showInCard(block: Rect, progress: Float, blockRadius: Float) {
    val card = cardBounds(block, size, progress)
    shape = CardShape(card, radius = lerp(blockRadius, 0f, progress))
    clip = true
    alpha = ((progress - ScreenFadeInFrom) / (1f - ScreenFadeInFrom)).coerceIn(0f, 1f)
}

// The card's rounded rectangle, at its place in the screen.
private data class CardShape(val card: Rect, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(card, CornerRadius(radius)))
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

// The card fades in over the block over this first part of the animation; going back, it fades
// out over the same last part.
private const val CardFadeIn = 0.15f

// The screen fades in from this point, when the card covers most of it; going back, it fades out
// until then, and the card shrinks plain.
private const val ScreenFadeInFrom = 0.8f
