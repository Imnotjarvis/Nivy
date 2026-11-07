package com.nivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import com.nivy.data.db.entity.BudgetEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import java.util.UUID
import javax.inject.Inject

class BudgetMapper @Inject constructor() {
    fun BudgetEntity.toDomain(): Either<String, Budget> = either {
        Budget(
            id = BudgetId(id),
            name = NotBlankTrimmedString.from(name).bind(),
            amount = PositiveDouble.from(amount).bind(),
            categories = parseIds(categoryIdsSerialized).map(::CategoryId),
            accounts = parseIds(accountIdsSerialized).map(::AccountId),
            orderNum = orderId,
        )
    }

    fun Budget.toEntity(): BudgetEntity = BudgetEntity(
        name = name.value,
        amount = amount.value,
        categoryIdsSerialized = categories.joinToString(separator = IdSeparator) { it.value.toString() },
        accountIdsSerialized = accounts.joinToString(separator = IdSeparator) { it.value.toString() },
        orderId = orderNum,
        id = id.value,
    )

    private fun parseIds(serialized: String?): List<UUID> = serialized
        ?.split(IdSeparator)
        ?.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: emptyList()

    private companion object {
        const val IdSeparator = ","
    }
}
