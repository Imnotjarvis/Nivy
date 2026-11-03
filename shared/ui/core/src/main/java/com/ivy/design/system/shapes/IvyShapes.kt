package com.ivy.design.system.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The one corner radius every block and pill in the app uses: the user's choice, or by default
 * one that matches the phone's screen corners. Shapes read it while composing, so changing it
 * redraws the app.
 */
object AppCornerRadius {
    /** Blocks keep this margin from the screen's edge, so matching its corners takes it off. */
    val ScreenMargin = 16.dp

    /** The radius the user picked in the settings, or null to match the phone. */
    var setting: Dp? by mutableStateOf(null)

    /** The screen's corner radius minus [ScreenMargin]; 16 dp on phones that don't report it. */
    var matchPhone: Dp by mutableStateOf(16.dp)

    val value: Dp get() = setting ?: matchPhone
}

/** Material's shapes, all with the app's corner radius. */
fun ivyShapes(radius: Dp): Shapes {
    val shape = RoundedCornerShape(radius)
    return Shapes(
        extraSmall = shape,
        small = shape,
        medium = shape,
        large = shape,
        extraLarge = shape,
    )
}

/** Bottom sheets, modals and card headers round only their top corners. */
val ShapeTop: Shape
    get() = RoundedCornerShape(topStart = AppCornerRadius.value, topEnd = AppCornerRadius.value)

/** Pills follow the app's corner radius; square elements (round icon buttons) stay circles. */
val PillShape: Shape get() = Pill(AppCornerRadius.value)

private data class Pill(val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val square = abs(size.width - size.height) <= size.minDimension * SquareTolerance
        val shape = if (square) CircleShape else RoundedCornerShape(radius)
        return shape.createOutline(size, layoutDirection, density)
    }
}

// Within this much of square counts as square: a round button whose padding isn't quite even
// stays a circle.
private const val SquareTolerance = 0.1f
