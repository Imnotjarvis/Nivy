package com.nivy.wallet.ui

import com.nivy.base.model.AccountType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.base.model.IntervalType
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.base.model.LoanType
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/** An account for previews and screenshot tests. */
fun previewAccount(
    name: String,
    color: Color,
    icon: String? = null,
    currency: String = "USD",
    includeInBalance: Boolean = true,
    id: UUID = UUID.randomUUID(),
): Account = Account(
    id = AccountId(id),
    name = NotBlankTrimmedString.unsafe(name),
    asset = AssetCode.unsafe(currency),
    color = ColorInt(color.toArgb()),
    icon = icon?.let(IconAsset::unsafe),
    includeInBalance = includeInBalance,
    type = AccountType.OTHER,
    archived = false,
    orderNum = 0.0,
)

/** A loan for previews and screenshot tests. */
fun previewLoan(
    name: String,
    amount: Double,
    color: Color,
    type: LoanType,
    dateTime: LocalDateTime? = null,
    icon: String? = null,
): Loan = Loan(
    id = LoanId(UUID.randomUUID()),
    name = NotBlankTrimmedString.unsafe(name),
    amount = PositiveDouble.unsafe(amount),
    type = type,
    color = ColorInt(color.toArgb()),
    icon = icon?.let(IconAsset::unsafe),
    account = null,
    note = null,
    dateTime = dateTime,
    orderNum = 0.0,
)

/** A loan record for previews and screenshot tests. */
fun previewLoanRecord(
    amount: Double,
    dateTime: Instant,
    type: LoanRecordType,
    note: String? = null,
): LoanRecord = LoanRecord(
    id = LoanRecordId(UUID.randomUUID()),
    loan = LoanId(UUID.randomUUID()),
    amount = PositiveDouble.unsafe(amount),
    note = note?.let(NotBlankTrimmedString::unsafe),
    dateTime = dateTime,
    interest = false,
    account = null,
    convertedAmount = null,
    type = type,
)

/** A planned payment rule for previews and screenshot tests; one-time without an [interval]. */
@Suppress("LongParameterList")
fun previewPlannedPaymentRule(
    account: AccountId,
    title: String,
    category: CategoryId?,
    amount: Double,
    startDate: Instant,
    type: TransactionType,
    interval: Pair<Int, IntervalType>? = null,
): PlannedPaymentRule = PlannedPaymentRule(
    id = PlannedPaymentRuleId(UUID.randomUUID()),
    type = type,
    account = account,
    amount = PositiveDouble.unsafe(amount),
    category = category,
    title = NotBlankTrimmedString.unsafe(title),
    description = null,
    startDate = startDate,
    interval = interval?.let { (n, intervalType) ->
        PlannedPaymentInterval(n = PositiveInt.unsafe(n), type = intervalType)
    },
    creditCard = null,
)

/**
 * A transaction for previews and screenshot tests: a transfer needs a [toAccount].
 * It's due at [time] unless [settled].
 */
@Suppress("LongParameterList")
fun previewTransaction(
    type: TransactionType,
    account: Account,
    amount: Double,
    time: Instant = Instant.now(),
    settled: Boolean = true,
    title: String? = null,
    category: CategoryId? = null,
    toAccount: Account? = null,
    toAmount: Double = amount,
    id: UUID = UUID.randomUUID(),
): Transaction {
    val value = PositiveValue(PositiveDouble.unsafe(amount), account.asset)
    val metadata = TransactionMetadata(
        recurringRuleId = null,
        paidForDateTime = null,
        loanId = null,
        loanRecordId = null,
        purchaseDateTime = null,
    )
    val transactionId = TransactionId(id)
    val notBlankTitle = title?.let(NotBlankTrimmedString::unsafe)
    return when (type) {
        TransactionType.INCOME -> Income(
            id = transactionId,
            title = notBlankTitle,
            description = null,
            category = category,
            time = time,
            settled = settled,
            metadata = metadata,
            tags = emptyList(),
            value = value,
            account = account.id,
        )

        TransactionType.EXPENSE -> Expense(
            id = transactionId,
            title = notBlankTitle,
            description = null,
            category = category,
            time = time,
            settled = settled,
            metadata = metadata,
            tags = emptyList(),
            value = value,
            account = account.id,
        )

        TransactionType.TRANSFER -> {
            val destination = requireNotNull(toAccount) { "A transfer needs a destination account" }
            Transfer(
                id = transactionId,
                title = notBlankTitle,
                description = null,
                category = category,
                time = time,
                settled = settled,
                metadata = metadata,
                tags = emptyList(),
                fromAccount = account.id,
                fromValue = value,
                toAccount = destination.id,
                toValue = PositiveValue(PositiveDouble.unsafe(toAmount), destination.asset),
            )
        }
    }
}
