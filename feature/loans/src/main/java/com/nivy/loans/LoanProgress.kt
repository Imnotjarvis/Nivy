package com.nivy.loans

import com.nivy.base.model.LoanRecordType
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanRecord

/**
 * How far a loan is paid back, the same on the loans list and on the loan's page. In the loan's
 * currency: a record in another one counts its converted amount.
 *
 * @property total the loan's amount, and every record that increased it.
 * @property paid what the records that decreased it paid back.
 * @property interestPaid what the records marked as interest paid, apart: interest doesn't pay
 * the loan back.
 */
data class LoanProgress(
    val total: Double,
    val paid: Double,
    val interestPaid: Double,
) {
    /** What's left to pay back. */
    val left: Double get() = total - paid

    /** [paid] as a share of [total], 1 when paid off; 0 while there's no total. */
    val percentPaid: Double get() = shareOfTotal(paid)

    /** [interestPaid] as a share of [total]; 0 while there's no total. */
    val interestPercentPaid: Double get() = shareOfTotal(interestPaid)

    private fun shareOfTotal(amount: Double): Double = if (total > 0) amount / total else 0.0

    companion object {
        /** Before the loan is there. */
        val None = LoanProgress(total = 0.0, paid = 0.0, interestPaid = 0.0)
    }
}

/**
 * The loan's progress with its [records]. Interest records stay out of the loan's total: one that
 * decreased it counts in [LoanProgress.interestPaid], and one that increased it, which the record
 * dialog no longer offers, counts for nothing.
 */
fun Loan.progress(records: List<LoanRecord>): LoanProgress {
    var total = amount.value
    var paid = 0.0
    var interestPaid = 0.0
    for (record in records) {
        val value = (record.convertedAmount ?: record.amount).value
        when {
            record.type == LoanRecordType.INCREASE -> if (!record.interest) total += value
            record.interest -> interestPaid += value
            else -> paid += value
        }
    }
    return LoanProgress(total = total, paid = paid, interestPaid = interestPaid)
}
