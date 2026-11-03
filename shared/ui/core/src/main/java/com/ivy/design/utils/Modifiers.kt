package com.ivy.design.utils

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

// Swallows clicks so they don't reach what's underneath (e.g. behind a modal).
fun Modifier.consumeClicks(interactionSource: MutableInteractionSource) =
    clickableNoIndication(interactionSource) {
        // consume click
    }
