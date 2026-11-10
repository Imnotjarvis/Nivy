package com.nivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.CurrencyRepository
import javax.inject.Inject

class AccountMapper @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend fun AccountEntity.toDomain(): Either<String, Account> = either {
        Account(
            id = AccountId(id),
            name = NotBlankTrimmedString.from(name).bind(),
            // An account stored without a currency is given the base currency for good, by a
            // migration and by the backup import; until then, it's read in the base currency.
            asset = currency?.let(AssetCode::from)?.getOrNull()
                ?: currencyRepository.getBaseCurrency(),
            color = ColorInt(color),
            icon = icon?.let(IconAsset::from)?.getOrNull(),
            includeInBalance = includeInBalance,
            type = type,
            archived = archived,
            orderNum = orderNum,
        )
    }

    fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            name = name.value,
            currency = asset.code,
            color = color.value,
            icon = icon?.id,
            orderNum = orderNum,
            includeInBalance = includeInBalance,
            type = type,
            archived = archived,
            id = id.value,
        )
    }
}
