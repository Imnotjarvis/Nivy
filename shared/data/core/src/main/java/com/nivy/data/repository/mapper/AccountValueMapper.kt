package com.nivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import com.nivy.data.db.entity.AccountValueEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.AccountValue
import com.nivy.data.model.AccountValueId
import com.nivy.data.model.primitive.NonNegativeDouble
import javax.inject.Inject

class AccountValueMapper @Inject constructor() {
    fun AccountValueEntity.toDomain(): Either<String, AccountValue> = either {
        AccountValue(
            id = AccountValueId(id),
            account = AccountId(accountId),
            time = time,
            value = NonNegativeDouble.from(value).bind(),
            cash = cash?.let { NonNegativeDouble.from(it).bind() },
        )
    }

    fun AccountValue.toEntity(): AccountValueEntity = AccountValueEntity(
        accountId = account.value,
        time = time,
        value = value.value,
        cash = cash?.value,
        id = id.value,
    )
}
