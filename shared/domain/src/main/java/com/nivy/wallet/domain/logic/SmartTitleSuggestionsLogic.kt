package com.nivy.wallet.domain.logic

import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.model.Category
import com.nivy.data.repository.CategoryRepository
import com.nivy.wallet.utils.capitalizeWords
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

const val SUGGESTIONS_LIMIT = 10

/** A title used before, and the category it's in most, if any is still there. */
data class TitleSuggestion(val title: String, val category: Category?)

class SmartTitleSuggestionsLogic @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository,
) {

    /**
     * Suggests titles based on:
     * - title match
     * - most used titles for categories
     * - if suggestions.size < SUGGESTIONS_LIMIT most used titles for accounts
     */
    suspend fun suggest(
        title: String?,
        categoryId: UUID?,
        accountId: UUID?
    ): List<TitleSuggestion> {
        // Each title with the category it's in most, most used first.
        val suggestions = linkedMapOf<String, UUID?>()

        if (!title.isNullOrEmpty()) {
            // suggest by title
            val suggestionsByTitle = transactionDao.findAllByTitleMatchingPattern("$title%")
                .titlesByUse()

            suggestions.putAll(suggestionsByTitle)
        }

        if (categoryId != null) {
            // suggest by category
            // all titles used for the specific category
            // ordered by N times used

            val suggestionsByCategory = transactionDao
                .findAllByCategory(
                    categoryId = categoryId
                )
                // exclude already suggested suggestions so they're ordered by priority at the end
                .titlesByUse(excludeSuggestions = suggestions.keys)

            suggestions.putAll(suggestionsByCategory)
        }

        if (suggestions.size < SUGGESTIONS_LIMIT && accountId != null) {
            // last resort, suggest by account
            // all titles used for the specific account
            // ordered by N times used

            val suggestionsByAccount = transactionDao
                .findAllByAccount(
                    accountId = accountId
                )
                // exclude already suggested suggestions so they're ordered by priority at the end
                .titlesByUse(excludeSuggestions = suggestions.keys)

            suggestions.putAll(suggestionsByAccount)
        }

        // A category deleted since brings none.
        val categories = categoryRepository.findAll().associateBy { it.id.value }
        return suggestions
            .filterKeys { it != title }
            .map { (suggestion, category) -> TitleSuggestion(suggestion, category?.let(categories::get)) }
    }
}

/**
 * The titles of these transactions, most used first, counted here rather than one query each, each
 * with the category it's in most.
 */
private fun List<TransactionEntity>.titlesByUse(
    excludeSuggestions: Set<String> = emptySet()
): Map<String, UUID?> = mapNotNull { transaction ->
    transaction.title?.takeIf(String::isNotBlank)?.let { it.trim().capitalizeWords() to transaction }
}
    .filterNot { (title, _) -> title in excludeSuggestions }
    .groupBy({ it.first }, { it.second })
    .entries.sortedByDescending { it.value.size }
    .associate { (title, transactions) -> title to transactions.mostUsedCategory() }

// The category these are in most; of those they're in as often, the one they were in last.
private fun List<TransactionEntity>.mostUsedCategory(): UUID? =
    mapNotNull { transaction -> transaction.categoryId?.let { it to transaction.dateTime } }
        .groupBy({ it.first }, { it.second })
        .entries
        .maxWithOrNull(
            compareBy<Map.Entry<UUID, List<Instant?>>> { it.value.size }
                .thenBy { it.value.filterNotNull().maxOrNull() }
        )
        ?.key
