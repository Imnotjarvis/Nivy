package com.nivy.categories

import com.nivy.wallet.domain.data.SortOrder

/** The categories whose name holds [query]; all of them while it's blank. */
internal fun List<CategoryData>.matching(query: String): List<CategoryData> =
    filter { it.category.name.value.contains(query.trim(), ignoreCase = true) }

/** The categories in [sortOrder]; by balance, those with a balance first. */
internal fun List<CategoryData>.inOrder(sortOrder: SortOrder): List<CategoryData> = when (sortOrder) {
    SortOrder.DEFAULT -> sortedBy { it.category.orderNum }

    SortOrder.BALANCE_AMOUNT -> sortedByDescending { it.monthlyBalance }
        .partition { it.monthlyBalance.toInt() != 0 }
        .let { (nonZero, zero) -> nonZero + zero }

    SortOrder.ALPHABETICAL -> sortedBy { it.category.name.value }

    SortOrder.EXPENSES -> sortedByDescending { it.monthlyExpenses }
}
