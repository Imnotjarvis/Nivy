package com.nivy.wallet.ui.component.transaction

import com.nivy.data.model.Account
import com.nivy.data.model.Category
import java.util.UUID

fun category(
    categoryId: UUID?,
    categories: List<Category>
): Category? {
    val targetId = categoryId ?: return null
    return categories.find { it.id.value == targetId }
}

fun account(
    accountId: UUID?,
    accounts: List<Account>
): Account? {
    val targetId = accountId ?: return null
    return accounts.find { it.id.value == targetId }
}
