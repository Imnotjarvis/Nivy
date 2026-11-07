package com.nivy.data.model

import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class BudgetId(override val value: UUID) : UniqueId

/**
 * A monthly spending limit, in the base currency.
 *
 * @property categories the categories it counts; empty means every category, a total budget.
 * @property accounts the accounts it counts; empty means every account.
 */
data class Budget(
    override val id: BudgetId,
    val name: NotBlankTrimmedString,
    val amount: PositiveDouble,
    val categories: List<CategoryId>,
    val accounts: List<AccountId>,
    override val orderNum: Double,
) : Identifiable<BudgetId>, Reorderable

/** Its expenses among [transactions]: of its accounts and categories, all of them when it has none. */
fun Budget.expensesIn(transactions: List<Transaction>): List<Expense> = transactions
    .filterIsInstance<Expense>()
    .filter { accounts.isEmpty() || it.account in accounts }
    .filter { categories.isEmpty() || it.category in categories }
