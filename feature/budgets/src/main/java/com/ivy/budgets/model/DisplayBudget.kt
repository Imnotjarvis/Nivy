package com.ivy.budgets.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Budget
import com.ivy.wallet.domain.data.Reorderable

@Immutable
data class DisplayBudget(
    val budget: Budget,
    val spentAmount: Double
) : Reorderable {
    override fun getItemOrderNum(): Double {
        return budget.orderNum
    }

    override fun withNewOrderNum(newOrderNum: Double): Reorderable {
        return this.copy(
            budget = budget.copy(
                orderNum = newOrderNum
            )
        )
    }
}
