package com.nivy.budgets

import com.nivy.budgets.model.DisplayBudget
import com.nivy.data.model.AccountId
import com.nivy.data.model.Budget
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble

sealed interface BudgetScreenEvent {
    data class OnReorder(val newOrder: List<DisplayBudget>) : BudgetScreenEvent
    data class OnCreateBudget(
        val name: NotBlankTrimmedString,
        val amount: PositiveDouble,
        val categories: List<CategoryId>,
        val accounts: List<AccountId>,
    ) : BudgetScreenEvent
    data class OnEditBudget(val budget: Budget) : BudgetScreenEvent
    data class OnDeleteBudget(val budget: Budget) : BudgetScreenEvent
    data class OnReorderModalVisible(val visible: Boolean) : BudgetScreenEvent
    data class OnBudgetModalData(val budgetModalData: BudgetModalData?) : BudgetScreenEvent
    data object OnPreviousMonth : BudgetScreenEvent
    data object OnNextMonth : BudgetScreenEvent
    data class OnSetSavingsGoal(val amount: Double) : BudgetScreenEvent
}
