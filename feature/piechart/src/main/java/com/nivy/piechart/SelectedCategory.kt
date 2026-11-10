package com.nivy.piechart

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Category

/** A selected slice of the chart by category: a category's, or Unspecified's when [category] is null. */
@Immutable
data class SelectedCategory(
    val category: Category?
)
