package com.nivy.wallet.domain.logic

import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.wallet.utils.capitalizeWords
import java.util.UUID
import javax.inject.Inject

const val SUGGESTIONS_LIMIT = 10

class SmartTitleSuggestionsLogic @Inject constructor(
    private val transactionDao: TransactionDao
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
    ): Set<String> {
        val suggestions = mutableSetOf<String>()

        if (!title.isNullOrEmpty()) {
            // suggest by title
            val suggestionsByTitle = transactionDao.findAllByTitleMatchingPattern("$title%")
                .titlesByUse()

            suggestions.addAll(suggestionsByTitle)
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
                .titlesByUse(excludeSuggestions = suggestions)

            suggestions.addAll(suggestionsByCategory)
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
                .titlesByUse(excludeSuggestions = suggestions)

            suggestions.addAll(suggestionsByAccount)
        }

        return suggestions
            .filter { it != title }
            .toSet()
    }
}

/** The titles of these transactions, most used first, counted here rather than one query each. */
private fun List<TransactionEntity>.titlesByUse(
    excludeSuggestions: Set<String> = emptySet()
): List<String> = mapNotNull { it.title?.takeIf(String::isNotBlank) }
    .map { it.trim().capitalizeWords() }
    .filterNot { it in excludeSuggestions }
    .groupingBy { it }.eachCount()
    .entries.sortedByDescending { it.value }
    .map { it.key }
