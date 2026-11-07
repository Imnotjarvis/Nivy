package com.nivy.budgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nivy.ui.R

@Composable
@ReadOnlyComposable
fun determineBudgetType(categoriesCount: Int): String {
    return when (categoriesCount) {
        0 -> stringResource(R.string.total_budget)
        1 -> stringResource(R.string.category_budget)
        else -> stringResource(R.string.multi_category_budget, categoriesCount.toString())
    }
}
