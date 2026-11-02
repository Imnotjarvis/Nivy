package com.ivy.domain.usecase.creditcard

import com.ivy.base.model.AccountType
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
import java.time.LocalDateTime
import java.time.YearMonth
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
     * its charge date. Anything else stays as it is.
     */
    suspend fun dated(transaction: Transaction): Transaction {
        if (transaction !is Expense || !transaction.settled) return transaction
        val account = accountRepository.findById(transaction.account) ?: return transaction
        val purchase = transaction.metadata.purchaseDateTime ?: transaction.time
        val charge = with(timeConverter) {
            account.chargeDateTime(purchase.toLocalDateTime())?.toUTC()
        } ?: return transaction
        return transaction.copy(
            time = charge,
            metadata = transaction.metadata.copy(purchaseDateTime = purchase),
        )
    }
}

/**
 * When the card charges a purchase made at [purchase]: on its charge day of the next month, at the
 * same time of day, or on that month's last day when it's shorter. Null when the account isn't a
 * credit card with a charge day.
 */
fun Account.chargeDateTime(purchase: LocalDateTime): LocalDateTime? {
    val day = chargeDay?.takeIf { type == AccountType.CREDIT_CARD } ?: return null
    val month = YearMonth.from(purchase).plusMonths(1)
    return month.atDay(minOf(day, month.lengthOfMonth())).atTime(purchase.toLocalTime())
}
