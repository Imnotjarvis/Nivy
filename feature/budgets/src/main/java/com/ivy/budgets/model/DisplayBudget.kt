package com.ivy.budgets.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Budget
import com.ivy.data.model.Expense
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.ivy.wallet.domain.data.Reorderable

/** A budget in a month: what it [spentAmount], in the base currency, and the [expenses] it counts. */
@Immutable
data class DisplayBudget(
    val budget: Budget,
    val spentAmount: Double,
    val expenses: ImmutableList<Expense> = persistentListOf(),
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
