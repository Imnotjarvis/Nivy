package com.nivy.design.utils

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

// Swallows clicks so they don't reach what's underneath (e.g. behind a modal).
fun Modifier.consumeClicks(interactionSource: MutableInteractionSource) =
    clickableNoIndication(interactionSource) {
        // consume click
    }

/**
 * A card's outline drawn behind its content, so a colored header covers it and it frames only
 * the rest of the card. Goes right after the card's clip to [shape], which cuts the outer half of
 * the stroke: [width] of it shows inside the edge.
 */
fun Modifier.outlineBehind(width: Dp, color: Color, shape: Shape) = drawBehind {
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        color = color,
        style = Stroke(width = width.toPx() * 2),
    )
}
