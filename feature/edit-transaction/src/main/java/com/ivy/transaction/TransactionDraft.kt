package com.ivy.transaction

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getFromValue
import com.ivy.data.model.getToAccount
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.data.temp.migration.getTransactionType
import java.time.Instant
import java.util.UUID

/**
 * The transaction the screen edits. Unlike [Transaction] it can be incomplete,
 * like a new one without an amount yet.
 */
@Suppress("DataClassDefaultValues")
internal data class TransactionDraft(
    val accountId: UUID,
    val type: TransactionType,
    val amount: Double,
    val toAccountId: UUID? = null,
    val toAmount: Double = amount,
    val title: String? = null,
    val description: String? = null,
    val dateTime: Instant? = null,
    val categoryId: UUID? = null,
    val dueDate: Instant? = null,
    val recurringRuleId: UUID? = null,
    val paidFor: Instant? = null,
    val loanId: UUID? = null,
    val loanRecordId: UUID? = null,
    val id: UUID = UUID.randomUUID(),
)

internal fun Transaction.toDraft(): TransactionDraft = TransactionDraft(
    accountId = getFromAccount().value,
    type = getTransactionType(),
    amount = getFromValue().amount.value,
    toAccountId = getToAccount()?.value,
    toAmount = (this as? Transfer)?.toValue?.amount?.value ?: getFromValue().amount.value,
    title = title?.value,
    description = description?.value,
    dateTime = time.takeIf { settled },
    categoryId = category?.value,
    dueDate = time.takeUnless { settled },
    recurringRuleId = metadata.recurringRuleId,
    paidFor = metadata.paidForDateTime,
    loanId = metadata.loanId,
    loanRecordId = metadata.loanRecordId,
    id = id.value,
)

/** The transaction to save, or null while the draft isn't a valid one. */
internal suspend fun TransactionDraft.toDomain(mapper: TransactionMapper): Transaction? {
    val entity = TransactionEntity(
        accountId = accountId,
        type = type,
        amount = amount,
        toAccountId = toAccountId,
        toAmount = toAmount,
        title = title,
        description = description,
        dateTime = dateTime,
        categoryId = categoryId,
        dueDate = dueDate,
        recurringRuleId = recurringRuleId,
        paidForDateTime = paidFor,
        loanId = loanId,
        loanRecordId = loanRecordId,
        id = id,
    )
    return with(mapper) { entity.toDomain() }.getOrNull()
}
