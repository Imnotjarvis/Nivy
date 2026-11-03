package com.ivy.domain.usecase.creditcard

import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
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
    private val accountRepository: AccountRepository,
    private val timeConverter: TimeConverter,
) {
    /**
     * The [transaction] as it's saved: a paid expense on a credit card with a charge day moves to
     * its charge date, moved as the user did. Anything else stays as it is.
     */
    suspend fun dated(transaction: Transaction): Transaction {
        if (transaction !is Expense || !transaction.settled) return transaction
        val account = accountRepository.findById(transaction.account) ?: return transaction
        val purchase = transaction.metadata.purchaseDateTime ?: transaction.time
        val charge = with(timeConverter) {
            account.chargeDateTime(purchase.toLocalDateTime(), transaction.metadata.chargeShift)?.toUTC()
        } ?: return transaction
        return transaction.copy(
            time = charge,
            metadata = transaction.metadata.copy(purchaseDateTime = purchase),
        )
    }
}

/**
 * When the card charges a purchase made at [purchase], at the same time of day; null when the
 * account isn't a credit card with a charge day. The purchase goes in the statement that closes
 * next (on its closing day, or the month's last day), and the card charges it on the first charge
 * day after that. [shift] moves the charge by months, like when the bank closed the statement on
 * another day, but never to the purchase's day or before it.
 */
fun Account.chargeDateTime(purchase: LocalDateTime, shift: Int = 0): LocalDateTime? {
    val day = chargeDay?.takeIf { type == AccountType.CREDIT_CARD } ?: return null
    val month = usualChargeMonth(purchase.toLocalDate(), day)
        .plusMonths(shift.coerceAtLeast(minChargeShift(purchase)).toLong())
    return month.atDayOrLast(day).atTime(purchase.toLocalTime())
}

/** How far [chargeDateTime] can move the charge of [purchase] earlier: 0 or fewer months. */
fun Account.minChargeShift(purchase: LocalDateTime): Int {
    val day = chargeDay?.takeIf { type == AccountType.CREDIT_CARD } ?: return 0
    val date = purchase.toLocalDate()
    val earliest = YearMonth.from(date.next(day, orSameDay = false))
    return -earliest.until(usualChargeMonth(date, day), ChronoUnit.MONTHS).toInt()
}

// The month the card charges a purchase made on [date], unless the user moved it.
private fun Account.usualChargeMonth(date: LocalDate, chargeDay: Int): YearMonth {
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
