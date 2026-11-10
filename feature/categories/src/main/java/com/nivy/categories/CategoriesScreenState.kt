package com.nivy.categories

import com.nivy.wallet.domain.data.SortOrder
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategoriesScreenState(
    val baseCurrency: String = "",
    val categories: ImmutableList<CategoryData> = persistentListOf(),
    // Without the search filter: sorting and reordering work on every category.
    val allCategories: ImmutableList<CategoryData> = persistentListOf(),
    val reorderModalVisible: Boolean = false,
    val categoryModalData: CategoryModalData? = null,
    val sortModalVisible: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val compactCategoriesModeEnabled: Boolean,
    val showCategorySearchBar: Boolean,

)
