package com.ivy.design.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Composable
fun <T> densityScope(densityScope: @Composable Density.() -> T): T {
    return with(LocalDensity.current) { densityScope() }
}

fun Modifier.thenIf(
    condition: Boolean,
    otherModifier: Modifier.() -> Modifier
): Modifier {
    // Cannot use Modifier#then() because it stacks the previous modifiers multiple times
    return if (condition) {
        this.otherModifier()
    } else {
        this
    }
}

fun Modifier.thenWhen(
    logic: Modifier.() -> Modifier?
): Modifier {
    return this.logic() ?: this
}

@Composable
fun rememberInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

fun Modifier.clickableNoIndication(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
): Modifier {
    return this.clickable(
        interactionSource = interactionSource,
        onClick = onClick,
        role = null,
        indication = null
    )
}

@Composable
fun Int.toDensityDp() = densityScope { toDp() }

@Composable
fun Float.toDensityDp() = densityScope { toDp() }

@Composable
fun Dp.toDensityPx() = densityScope { toPx() }

// A text field value with the cursor after the last character.
fun selectEndTextFieldValue(text: String?) = TextFieldValue(
    text = text ?: "",
    selection = TextRange(text?.length ?: 0)
)
