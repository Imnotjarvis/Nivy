package com.ivy.data.model

import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
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
