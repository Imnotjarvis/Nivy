package com.nivy.wallet.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

fun <T> springBounce(
    stiffness: Float = 500f,
) = spring<T>(
    dampingRatio = 0.75f,
    stiffness = stiffness,
)

fun <T> springBounceSlow() = spring<T>(
    dampingRatio = 0.75f,
    stiffness = Spring.StiffnessVeryLow,
)
