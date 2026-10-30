package com.ivy.budgets

import com.ivy.budgets.model.DisplayBudget
import com.ivy.data.model.Budget
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble

sealed interface BudgetScreenEvent {
    data class OnReorder(val newOrder: List<DisplayBudget>) : BudgetScreenEvent
    data class OnCreateBudget(
        val name: NotBlankTrimmedString,
        val amount: PositiveDouble,
        val categories: List<CategoryId>,
    ) : BudgetScreenEvent
    data class OnEditBudget(val budget: Budget) : BudgetScreenEvent
    data class OnDeleteBudget(val budget: Budget) : BudgetScreenEvent
    data class OnReorderModalVisible(val visible: Boolean) : BudgetScreenEvent
    data class OnBudgetModalData(val budgetModalData: BudgetModalData?) : BudgetScreenEvent
}
