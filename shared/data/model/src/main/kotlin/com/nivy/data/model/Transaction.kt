package com.nivy.data.model

import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import java.time.Instant
import java.util.UUID

@JvmInline
value class TransactionId(override val value: UUID) : UniqueId

sealed interface Transaction : Identifiable<TransactionId> {
    override val id: TransactionId
    val title: NotBlankTrimmedString?
    val description: NotBlankTrimmedString?
    val category: CategoryId?
    val time: Instant
    val settled: Boolean
    val metadata: TransactionMetadata
}

data class Income(
    override val id: TransactionId,
    override val title: NotBlankTrimmedString?,
    override val description: NotBlankTrimmedString?,
    override val category: CategoryId?,
    override val time: Instant,
    override val settled: Boolean,
    override val metadata: TransactionMetadata,
    val value: PositiveValue,
    val account: AccountId,
) : Transaction

data class Expense(
    override val id: TransactionId,
    override val title: NotBlankTrimmedString?,
    override val description: NotBlankTrimmedString?,
    override val category: CategoryId?,
    override val time: Instant,
    override val settled: Boolean,
    override val metadata: TransactionMetadata,
    val value: PositiveValue,
    val account: AccountId,
) : Transaction

data class Transfer(
    override val id: TransactionId,
    override val title: NotBlankTrimmedString?,
    override val description: NotBlankTrimmedString?,
    override val category: CategoryId?,
    override val time: Instant,
    override val settled: Boolean,
    override val metadata: TransactionMetadata,
    val fromAccount: AccountId,
    val fromValue: PositiveValue,
    val toAccount: AccountId,
    val toValue: PositiveValue,
) : Transaction

data class TransactionMetadata(
    val recurringRuleId: UUID?,
    val paidForDateTime: Instant?,
    // This refers to the loan id that is linked with a transaction
    val loanId: UUID? = null,
    // This refers to the loan record id that is linked with a transaction
    val loanRecordId: UUID?,
    // When a credit card expense was bought; its time is then the date the card charges it.
    val purchaseDateTime: Instant?,
    // The months the user moved a credit card expense's charge from its usual date: + later, - earlier.
    val chargeShift: Int = 0,
    // The credit card an expense is paid with; null when it's paid from the account.
    val creditCard: CreditCardId? = null,
)

fun Transaction.getFromValue(): PositiveValue = when (this) {
    is Expense -> value
    is Income -> value
    is Transfer -> fromValue
}

fun Transaction.getFromAccount(): AccountId = when (this) {
    is Expense -> account
    is Income -> account
    is Transfer -> fromAccount
}

fun Transaction.getToAccount(): AccountId? = when (this) {
    is Expense -> null
    is Income -> null
    is Transfer -> toAccount
}
