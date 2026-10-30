package com.ivy.design.system.shapes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Ivy's corner radii: 16 dp for cards and buttons (medium), 20 dp (large) and 24 dp
// (extraLarge) for bigger surfaces. Pills and circles use CircleShape.
val IvyShapes = Shapes(
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Bottom sheets and modals round only their top corners.
val ShapeTop16 = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
val ShapeTop24 = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val ShapeTop32 = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
