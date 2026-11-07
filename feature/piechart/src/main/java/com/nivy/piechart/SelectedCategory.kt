package com.nivy.piechart

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Category

@Immutable
data class SelectedCategory(
    val category: Category // null - Unspecified
)
