package com.ivy.wallet.domain.logic

import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.wallet.utils.capitalizeWords
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
                .uniqueTitles()
                .sortedByMostUsedFirst {
                    transactionDao.countByTitleMatchingPattern("$it%")
                }

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
                .uniqueTitles(excludeSuggestions = suggestions)
                .sortedByMostUsedFirst {
                    transactionDao.countByTitleMatchingPatternAndCategoryId(
                        pattern = it,
                        categoryId = categoryId
                    )
                }

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
                .uniqueTitles(excludeSuggestions = suggestions)
                .sortedByMostUsedFirst {
                    transactionDao.countByTitleMatchingPatternAndAccountId(
                        pattern = it,
                        accountId = accountId
                    )
                }

            suggestions.addAll(suggestionsByAccount)
        }

        return suggestions
            .filter { it != title }
            .toSet()
    }
}

private fun List<TransactionEntity>.uniqueTitles(
    excludeSuggestions: Set<String> = emptySet()
): Set<String> = mapNotNull { it.title?.takeIf(String::isNotBlank) }
    .map { it.trim().capitalizeWords() }
    .filterNot { it in excludeSuggestions }
    .toSet()

private suspend fun Set<String>.sortedByMostUsedFirst(countUses: suspend (String) -> Long): Set<String> {
    val uses = associateWith { countUses(it) }
    return sortedByDescending { uses.getValue(it) }.toSet()
}
