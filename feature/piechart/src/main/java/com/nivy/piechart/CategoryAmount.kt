package com.nivy.piechart

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Category
import com.nivy.data.model.Transaction

@Immutable
data class CategoryAmount(
    val category: Category?,
    val amount: Double,
    val associatedTransactions: List<Transaction> = emptyList(),
    val isCategoryUnspecified: Boolean = false
)
