package com.ivy.search

import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import kotlinx.collections.immutable.ImmutableList

data class SearchState(
    val searchQuery: String,
    val transactions: ImmutableList<TransactionHistoryItem>,
    val baseCurrency: String,
    val accounts: ImmutableList<Account>,
    val categories: ImmutableList<Category>,
    val shouldShowAccountSpecificColorInTransactions: Boolean
)
