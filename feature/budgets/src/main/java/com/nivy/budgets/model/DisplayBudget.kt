package com.nivy.budgets.model

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Budget
import com.nivy.wallet.domain.data.Reorderable

/** A budget in a month: what it [spentAmount], in the base currency. */
@Immutable
data class DisplayBudget(
    val budget: Budget,
    val spentAmount: Double,
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
