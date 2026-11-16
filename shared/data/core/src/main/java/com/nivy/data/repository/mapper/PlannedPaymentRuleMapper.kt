package com.nivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import javax.inject.Inject

class PlannedPaymentRuleMapper @Inject constructor() {
    fun PlannedPaymentRuleEntity.toDomain(): Either<String, PlannedPaymentRule> = either {
        // A transfer goes to another account; an income's or an expense's has no meaning.
        val toAccount = if (type == TransactionType.TRANSFER) {
            ensureNotNull(toAccountId) { "Planned transfer $id has no account to go to" }
                .also { ensure(it != accountId) { "Planned transfer $id goes to the account it leaves" } }
        } else {
            null
        }
        PlannedPaymentRule(
            id = PlannedPaymentRuleId(id),
            type = type,
            account = AccountId(accountId),
            toAccount = toAccount?.let(::AccountId),
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
            // Paid once, it has nothing to end.
            endDate = endDate.takeUnless { oneTime },
            creditCard = creditCardId?.let(::CreditCardId),
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
        creditCardId = creditCard?.value,
        toAccountId = toAccount?.value,
        endDate = endDate,
        id = id.value,
    )
}
