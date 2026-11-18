package com.nivy.search

import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.domain.data.TransactionHistoryItem
import kotlinx.collections.immutable.ImmutableList

data class SearchState(
    // What [transactions] were found for; blank, none are, as nothing is listed before a query.
    val searchQuery: String,
    // The matches paid, by day.
    val transactions: ImmutableList<TransactionHistoryItem>,
    // The matches still to pay, as an account's page lists them: due from today on, and missed.
    val upcoming: DueSection,
    val overdue: DueSection,
    // What the matches paid came to in the base currency, without transfers.
    val income: Double,
    val expenses: Double,
    val baseCurrency: String,
    val accounts: ImmutableList<Account>,
    val categories: ImmutableList<Category>,
    val shouldShowAccountSpecificColorInTransactions: Boolean,
    val compactTransactions: Boolean = false,
)
