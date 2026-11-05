package com.ivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.model.CreditCardId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import javax.inject.Inject

class CreditCardMapper @Inject constructor() {
    fun CreditCardEntity.toDomain(): Either<String, CreditCard> = either {
        ensure(chargeDay in 1..MaxDayOfMonth) { "Credit card $id has no charge day" }
        CreditCard(
            id = CreditCardId(id),
            account = AccountId(accountId),
            name = NotBlankTrimmedString.from(name).bind(),
            chargeDay = chargeDay,
            statementClosingDay = statementClosingDay?.takeIf { it in 1 until MaxDayOfMonth },
            creditLimit = creditLimit?.let(PositiveDouble::from)?.getOrNull(),
        )
    }

    fun CreditCard.toEntity(): CreditCardEntity = CreditCardEntity(
        accountId = account.value,
        name = name.value,
        chargeDay = chargeDay,
        statementClosingDay = statementClosingDay,
        creditLimit = creditLimit?.value,
        id = id.value,
    )
}

private const val MaxDayOfMonth = 31
