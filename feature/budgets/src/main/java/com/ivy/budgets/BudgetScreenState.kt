package com.ivy.budgets

import com.ivy.budgets.model.DisplayBudget
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.wallet.data.model.FromToTimeRange
import com.ivy.wallet.data.model.TimePeriod
import kotlinx.collections.immutable.ImmutableList
import javax.annotation.concurrent.Immutable

@Immutable
data class BudgetScreenState(
    val baseCurrency: String,
    val budgets: ImmutableList<DisplayBudget>,
    val categories: ImmutableList<Category>,
    val accounts: ImmutableList<Account>,
    val savings: SavingsGoal,
    val timeRange: FromToTimeRange?,
    val period: TimePeriod?,
    val reorderModalVisible: Boolean,
    val budgetModalData: BudgetModalData?
)

/** The monthly savings goal, [goal] (none when 0), and what the month [saved] so far. */
@Immutable
data class SavingsGoal(val goal: Double, val saved: Double)
