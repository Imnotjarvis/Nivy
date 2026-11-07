package com.ivy.transaction

import android.content.Context
import com.ivy.data.model.Category
import com.ivy.data.repository.CategoryRepository
import com.ivy.domain.features.Features
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/** The categories to choose from, by name when the setting says so. */
class SortedCategories @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val features: Features,
    @ApplicationContext
    private val context: Context,
) {
    suspend fun sortCategories(): ImmutableList<Category> {
        val categories = categoryRepository.findAll()
        return if (shouldSortCategoriesAscending()) {
            categories.sortedBy { it.name.value }.toImmutableList()
        } else {
            categories.toImmutableList()
        }
    }

    private suspend fun shouldSortCategoriesAscending(): Boolean {
        return features.sortCategoriesAscending.isEnabled(context)
    }
}
