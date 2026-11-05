package com.ivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import com.ivy.data.db.entity.AccountValueEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountValue
import com.ivy.data.model.AccountValueId
import com.ivy.data.model.primitive.NonNegativeDouble
import javax.inject.Inject

class AccountValueMapper @Inject constructor() {
    fun AccountValueEntity.toDomain(): Either<String, AccountValue> = either {
        AccountValue(
            id = AccountValueId(id),
            account = AccountId(accountId),
            time = time,
            value = NonNegativeDouble.from(value).bind(),
        )
    }

    fun AccountValue.toEntity(): AccountValueEntity = AccountValueEntity(
        accountId = account.value,
        time = time,
        value = value.value,
        id = id.value,
    )
}
