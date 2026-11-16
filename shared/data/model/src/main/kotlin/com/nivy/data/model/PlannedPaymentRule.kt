package com.nivy.data.model

import com.nivy.base.model.IntervalType
import com.nivy.base.model.TransactionType
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.primitive.PositiveInt
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import java.time.Instant
import java.util.UUID

@JvmInline
value class PlannedPaymentRuleId(override val value: UUID) : UniqueId

/**
 * A planned income, expense or transfer, paid once or every [interval].
 *
 * @property type [TransactionType.INCOME], [TransactionType.EXPENSE] or [TransactionType.TRANSFER].
 * @property amount in the currency of [account]; for a transfer, what leaves it.
 * @property toAccount the account a transfer goes to; null for an income or an expense.
 * @property interval null for a one-time payment.
 * @property endDate the last day a recurring one can fall on; null when it doesn't end.
 * @property creditCard the card an expense or a transfer is paid with; null when it's paid from [account].
 */
data class PlannedPaymentRule(
    override val id: PlannedPaymentRuleId,
    val type: TransactionType,
    val account: AccountId,
    val toAccount: AccountId?,
    val amount: PositiveDouble,
    val category: CategoryId?,
    val title: NotBlankTrimmedString?,
    val description: NotBlankTrimmedString?,
    val startDate: Instant,
    val interval: PlannedPaymentInterval?,
    val endDate: Instant?,
    val creditCard: CreditCardId?,
) : Identifiable<PlannedPaymentRuleId>

data class PlannedPaymentInterval(
    val n: PositiveInt,
    val type: IntervalType,
)
