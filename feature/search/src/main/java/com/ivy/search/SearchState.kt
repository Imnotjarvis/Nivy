package com.ivy.search

import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.wallet.domain.data.TransactionHistoryItem
import kotlinx.collections.immutable.ImmutableList

data class SearchState(
    // What [transactions] were found for; blank, none are, as nothing is listed before a query.
    val searchQuery: String,
    val transactions: ImmutableList<TransactionHistoryItem>,
    val baseCurrency: String,
    val accounts: ImmutableList<Account>,
    val categories: ImmutableList<Category>,
    val shouldShowAccountSpecificColorInTransactions: Boolean,
    val compactTransactions: Boolean = false,
)
