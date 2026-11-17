package com.nivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.nivy.base.model.TransactionType
import com.nivy.data.db.entity.QuickAddEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import javax.inject.Inject

class QuickAddMapper @Inject constructor() {
    fun QuickAddEntity.toDomain(): Either<String, QuickAdd> = either {
        // A transfer goes to another account; an income's or an expense's has no meaning.
        val toAccount = if (type == TransactionType.TRANSFER) {
            ensureNotNull(toAccountId) { "Quick add $id is a transfer to no account" }
                .also { ensure(it != accountId) { "Quick add $id goes to the account it leaves" } }
        } else {
            null
        }
        QuickAdd(
            id = QuickAddId(id),
            type = type,
            account = AccountId(accountId),
            toAccount = toAccount?.let(::AccountId),
            amount = amount?.let { PositiveDouble.from(it).bind() },
            category = categoryId?.let(::CategoryId),
            title = title?.let(NotBlankTrimmedString::from)?.getOrNull(),
            creditCard = creditCardId?.let(::CreditCardId),
            orderNum = orderNum,
        )
    }

    fun QuickAdd.toEntity(): QuickAddEntity = QuickAddEntity(
        type = type,
        accountId = account.value,
        amount = amount?.value,
        categoryId = category?.value,
        title = title?.value,
        creditCardId = creditCard?.value,
        toAccountId = toAccount?.value,
        orderNum = orderNum,
        id = id.value,
    )
}
