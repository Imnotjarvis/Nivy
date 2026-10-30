package com.ivy.design.system.colors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Gradient(
    val startColor: Color,
    val endColor: Color
) {
    companion object {
        fun from(startColor: Int, endColor: Int?) = Gradient(
            startColor = startColor.toComposeColor(),
            endColor = (endColor ?: startColor).toComposeColor()
        )

        fun solid(color: Color) = Gradient(color, color)

        @Composable
        fun black() = Gradient(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurface
        )
    }

    fun asHorizontalBrush() = Brush.horizontalGradient(colors = listOf(startColor, endColor))

    fun asVerticalBrush() = Brush.verticalGradient(colors = listOf(startColor, endColor))
}

val GradientRed = Gradient(Red, Color(0xFFFF99AB))
val GradientPurple = Gradient(Violet, Color(0xFFED3EF7))
val GradientGreen = Gradient(Green, Color(0xFF49F2C8))
val GradientOrangeRevert = Gradient(Color(0xFFF2CD9E), Orange)
val GradientIvy = Gradient(Ivy, Color(0xFFAA99FF))

@Composable
fun gradientExpenses() = Gradient(
    MaterialTheme.colorScheme.onSurface,
    MaterialTheme.colorScheme.onSurfaceVariant
)

// The screen background, slightly see-through, for bars that float over scrolling content.
@Composable
fun pureBlur() = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

@Composable
fun mediumBlur() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)

// Fades content into the background at the top or bottom edge.
fun Modifier.gradientCutBackgroundTop(
    pure: Color,
    density: Density,
    endY: Dp = 32.dp
): Modifier {
    return background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Transparent,
                pure,
            ),
            endY = with(density) {
                endY.toPx()
            }
        )
    ).padding(top = 16.dp)
}

fun Modifier.gradientCutBackgroundBottom(
    pure: Color,
    paddingBottom: Dp,
): Modifier {
    return background(
        brush = Brush.verticalGradient(
            colors = listOf(
                pure,
                Transparent
            ),
        )
    ).padding(bottom = paddingBottom)
}
