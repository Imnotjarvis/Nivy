package com.ivy.accounts

import com.ivy.base.model.AccountType

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<com.ivy.wallet.data.model.AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent
    data class OnTypeFilter(val type: AccountType?) : AccountsEvent
    data class OnShowArchived(val show: Boolean) : AccountsEvent
}
