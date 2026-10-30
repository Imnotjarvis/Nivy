package com.ivy.legacy.ui.component.transaction

import androidx.compose.runtime.Composable
import com.ivy.data.model.Category
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import java.util.UUID

@Composable
fun category(
    categoryId: UUID?,
    categories: List<Category>
): Category? {
    val targetId = categoryId ?: return null
    return ivyWalletCtx().categoryMap[targetId] ?: categories.find { it.id.value == targetId }
}

@Composable
fun account(
    accountId: UUID?,
    accounts: List<Account>
): Account? {
    val targetId = accountId ?: return null
    return ivyWalletCtx().accountMap[targetId] ?: accounts.find { it.id == targetId }
}
