package com.ivy.data.model

import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class CreditCardId(override val value: UUID) : UniqueId

/**
 * A credit card that [account] pays: an expense paid with it counts on the day the card charges it.
 *
 * @property chargeDay the day of the month (1-31) the card charges the last statement's purchases.
 * @property statementClosingDay the day of the month (1-30) its statement closes; null for the
 * month's last day.
 */
data class CreditCard(
    override val id: CreditCardId,
    val account: AccountId,
    val name: NotBlankTrimmedString,
    val chargeDay: Int,
    val statementClosingDay: Int?,
    val creditLimit: PositiveDouble?,
) : Identifiable<CreditCardId>
