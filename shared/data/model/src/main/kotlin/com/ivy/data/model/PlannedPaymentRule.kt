package com.ivy.data.model

import com.ivy.base.model.TransactionType
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.primitive.PositiveInt
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import java.time.Instant
import java.util.UUID

@JvmInline
value class PlannedPaymentRuleId(override val value: UUID) : UniqueId

/**
 * A planned income or expense, paid once or every [interval].
 *
 * @property type [TransactionType.INCOME] or [TransactionType.EXPENSE].
 * @property amount in the currency of [account].
 * @property interval null for a one-time payment.
 */
data class PlannedPaymentRule(
    override val id: PlannedPaymentRuleId,
    val type: TransactionType,
    val account: AccountId,
    val amount: PositiveDouble,
    val category: CategoryId?,
    val title: NotBlankTrimmedString?,
    val description: NotBlankTrimmedString?,
    val startDate: Instant,
    val interval: PlannedPaymentInterval?,
) : Identifiable<PlannedPaymentRuleId>

data class PlannedPaymentInterval(
    val n: PositiveInt,
    val type: IntervalType,
)
