package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.base.model.AccountType
import com.ivy.wallet.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class AccountsState(
    val baseCurrency: String,
    // Every account, for reordering; the list shows [shownAccountsData].
    val accountsData: ImmutableList<AccountData>,
    val shownAccountsData: ImmutableList<AccountData>,
    // The types of the unarchived accounts, to filter by; null shows them all.
    val accountTypes: ImmutableList<AccountType>,
    val typeFilter: AccountType?,
    val archivedCount: Int,
    val showArchived: Boolean,
    val totalBalanceWithExcluded: String,
    val totalBalanceWithExcludedText: String,
    val totalBalanceWithoutExcluded: String,
    val totalBalanceWithoutExcludedText: String,
    val reorderVisible: Boolean,
    val compactAccountsModeEnabled: Boolean,
    val hideTotalBalance: Boolean,
    // Whether the accounts were loaded yet: until then, the totals show nothing.
    val loaded: Boolean = false,
)
