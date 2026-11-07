package com.nivy.domain.usecase.creditcard

import com.nivy.base.time.TimeConverter
import com.nivy.data.model.CreditCard
import com.nivy.data.model.Expense
import com.nivy.data.model.Transaction
import com.nivy.data.repository.CreditCardRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Dates the expenses paid with a credit card on the day the card charges them, so that they
 * count in that month's balance, budgets and reports. The purchase date stays in the metadata.
 */
class CreditCardChargeUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val timeConverter: TimeConverter,
) {
    /**
     * The [transaction] as it's saved: a paid expense on a credit card moves to its charge date,
     * moved as the user did. Anything else stays as it is.
     */
    suspend fun dated(transaction: Transaction): Transaction {
        if (transaction !is Expense || !transaction.settled) return transaction
        val card = transaction.metadata.creditCard?.let { creditCardRepository.findById(it) }
            ?: return transaction
        val purchase = transaction.metadata.purchaseDateTime ?: transaction.time
        val charge = with(timeConverter) {
            card.chargeDateTime(purchase.toLocalDateTime(), transaction.metadata.chargeShift).toUTC()
        }
        return transaction.copy(
            time = charge,
            metadata = transaction.metadata.copy(purchaseDateTime = purchase),
        )
    }
}

/**
 * When the card charges a purchase made at [purchase], at the same time of day. The purchase goes
 * in the statement that closes next (on its closing day, or the month's last day), and the card
 * charges it on the first charge day after that. [shift] moves the charge by months, like when the
 * bank closed the statement on another day, but never to the purchase's day or before it.
 */
fun CreditCard.chargeDateTime(purchase: LocalDateTime, shift: Int = 0): LocalDateTime {
    val month = usualChargeMonth(purchase.toLocalDate())
        .plusMonths(shift.coerceAtLeast(minChargeShift(purchase)).toLong())
    return month.atDayOrLast(chargeDay).atTime(purchase.toLocalTime())
}

/** How far [chargeDateTime] can move the charge of [purchase] earlier: 0 or fewer months. */
fun CreditCard.minChargeShift(purchase: LocalDateTime): Int {
    val date = purchase.toLocalDate()
    val earliest = YearMonth.from(date.next(chargeDay, orSameDay = false))
    return -earliest.until(usualChargeMonth(date), ChronoUnit.MONTHS).toInt()
}

// The month the card charges a purchase made on [date], unless the user moved it.
private fun CreditCard.usualChargeMonth(date: LocalDate): YearMonth {
    val closing = date.next(statementClosingDay ?: LastDayOfMonth, orSameDay = true)
    return YearMonth.from(closing.next(chargeDay, orSameDay = false))
}

// The next date on [day] of its month (or the month's last day, when it's shorter).
private fun LocalDate.next(day: Int, orSameDay: Boolean): LocalDate {
    val month = YearMonth.from(this)
    val thisMonth = month.atDayOrLast(day)
    val ahead = thisMonth > this || orSameDay && thisMonth == this
    return if (ahead) thisMonth else month.plusMonths(1).atDayOrLast(day)
}

private fun YearMonth.atDayOrLast(day: Int): LocalDate = atDay(minOf(day, lengthOfMonth()))

private const val LastDayOfMonth = 31
