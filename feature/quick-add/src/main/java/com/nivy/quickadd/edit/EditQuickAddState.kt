package com.nivy.quickadd.edit

import androidx.compose.runtime.Immutable
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CreditCard
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class EditQuickAddState(
    val type: TransactionType,
    val initialTitle: String?,
    val currency: String,
    val categories: ImmutableList<Category>,
    // The categories this type of transaction was put in most over the last 12 months.
    val mostUsedCategories: ImmutableList<Category>,
    val accounts: ImmutableList<Account>,
    val account: Account?,
    // The account a transfer goes to; null for an income or an expense.
    val toAccount: Account?,
    // The account's credit cards an expense or a transfer can be paid with, and the one it's paid with.
    val creditCards: ImmutableList<CreditCard>,
    val creditCard: CreditCard?,
    val category: Category?,
    // 0 when it's entered each time.
    val amount: Double,
    // Whether the form makes a quick add: an account, and for a transfer another one to go to.
    val canSave: Boolean,
)

sealed interface EditQuickAddEvent {
    data class OnTypeChanged(val type: TransactionType) : EditQuickAddEvent
    data class OnTitleChanged(val title: String?) : EditQuickAddEvent
    data class OnAmountChanged(val amount: Double) : EditQuickAddEvent
    data class OnAccountChanged(val account: Account) : EditQuickAddEvent
    data class OnToAccountChanged(val account: Account) : EditQuickAddEvent
    data class OnCreditCardChanged(val creditCard: CreditCard?) : EditQuickAddEvent
    data class OnCategoryChanged(val category: Category?) : EditQuickAddEvent
    data class OnCreateCategory(val data: CreateCategoryData) : EditQuickAddEvent
    data class OnEditCategory(val category: Category) : EditQuickAddEvent
    data class OnCreateAccount(val data: CreateAccountData) : EditQuickAddEvent
    data object OnSave : EditQuickAddEvent
    data object OnDelete : EditQuickAddEvent
}
