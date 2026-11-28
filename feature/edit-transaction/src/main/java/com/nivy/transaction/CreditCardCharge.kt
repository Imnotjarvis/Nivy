package com.nivy.transaction

import com.nivy.base.model.TransactionType
import com.nivy.data.model.CreditCard
import com.nivy.domain.usecase.creditcard.chargeDateTime
import com.nivy.domain.usecase.creditcard.minChargeShift
import java.time.Instant
import java.time.LocalDateTime

/** When the card charges the expense or transfer, if it's paid with a credit card and not planned. */
internal fun chargeDate(
    transactionType: TransactionType,
    dueDate: Instant?,
    creditCard: CreditCard?,
    chargeShift: Int,
    purchase: () -> LocalDateTime,
): LocalDateTime? {
    if (transactionType == TransactionType.INCOME || dueDate != null) return null
    return creditCard?.chargeDateTime(purchase(), chargeShift)
}

/** Whether the charge can move a month earlier without falling on or before the purchase. */
internal fun canMoveChargeEarlier(
    creditCard: CreditCard?,
    chargeShift: Int,
    purchase: () -> LocalDateTime,
): Boolean {
    val card = creditCard ?: return false
    return chargeShift > card.minChargeShift(purchase())
}
