package com.nivy.domain.usecase.transaction

import com.nivy.base.model.TransactionType
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.NotBlankTrimmedString
import java.time.Instant
import java.util.UUID

/**
 * A new transaction of [value] on [account]: a transfer when what arrives on [toAccount] is
 * known, else an income or an expense, as [type] says.
 */
internal fun newTransaction(
    type: TransactionType,
    account: AccountId,
    value: PositiveValue,
    title: NotBlankTrimmedString?,
    category: CategoryId?,
    time: Instant,
    settled: Boolean,
    metadata: TransactionMetadata,
    description: NotBlankTrimmedString? = null,
    toAccount: AccountId? = null,
    toValue: PositiveValue? = null,
): Transaction {
    val id = TransactionId(UUID.randomUUID())
    return when {
        toAccount != null && toValue != null -> Transfer(
            id = id, title = title, description = description, category = category, time = time,
            settled = settled, metadata = metadata,
            fromAccount = account, fromValue = value, toAccount = toAccount, toValue = toValue,
        )

        type == TransactionType.INCOME -> Income(
            id = id, title = title, description = description, category = category, time = time,
            settled = settled, metadata = metadata, value = value, account = account,
        )

        else -> Expense(
            id = id, title = title, description = description, category = category, time = time,
            settled = settled, metadata = metadata, value = value, account = account,
        )
    }
}
