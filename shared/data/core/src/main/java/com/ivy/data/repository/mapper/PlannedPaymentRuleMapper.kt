package com.ivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.CategoryId
import com.ivy.data.model.PlannedPaymentInterval
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.PositiveInt
import javax.inject.Inject

class PlannedPaymentRuleMapper @Inject constructor() {
    fun PlannedPaymentRuleEntity.toDomain(): Either<String, PlannedPaymentRule> = either {
        ensure(type != TransactionType.TRANSFER) { "Planned payment $id is a transfer" }
        PlannedPaymentRule(
            id = PlannedPaymentRuleId(id),
            type = type,
            account = AccountId(accountId),
            amount = PositiveDouble.from(amount).bind(),
            category = categoryId?.let(::CategoryId),
            title = title?.let(NotBlankTrimmedString::from)?.getOrNull(),
            description = description?.let(NotBlankTrimmedString::from)?.getOrNull(),
            startDate = ensureNotNull(startDate) { "Planned payment $id has no start date" },
            interval = if (oneTime) {
                null
            } else {
                PlannedPaymentInterval(
                    n = PositiveInt.from(
                        ensureNotNull(intervalN) { "Planned payment $id has no interval" }
                    ).bind(),
                    type = ensureNotNull(intervalType) { "Planned payment $id has no interval type" },
                )
            },
        )
    }

    fun PlannedPaymentRule.toEntity(): PlannedPaymentRuleEntity = PlannedPaymentRuleEntity(
        startDate = startDate,
        intervalN = interval?.n?.value,
        intervalType = interval?.type,
        oneTime = interval == null,
        type = type,
        accountId = account.value,
        amount = amount.value,
        categoryId = category?.value,
        title = title?.value,
        description = description?.value,
        id = id.value,
    )
}
