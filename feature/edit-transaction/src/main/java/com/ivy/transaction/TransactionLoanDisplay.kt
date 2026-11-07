package com.ivy.transaction

import android.content.Context
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.ui.R
import com.ivy.wallet.data.EditTransactionDisplayLoan
import com.ivy.wallet.utils.ioThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** What the editor says about the loan a transaction belongs to. */
class TransactionLoanDisplay @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val loanDao: LoanDao,
) {
    internal suspend fun getDisplayLoanHelper(trans: TransactionDraft): EditTransactionDisplayLoan {
        if (trans.loanId == null) {
            return EditTransactionDisplayLoan()
        }

        val loan =
            ioThread { loanDao.findById(trans.loanId) } ?: return EditTransactionDisplayLoan()
        val isLoanRecord = trans.loanRecordId != null

        val loanWarningDescription = if (isLoanRecord) {
            context.getString(
                R.string.note_transaction_associated_with_loan_record_of_loan,
                loan.name
            )
        } else {
            context.getString(
                R.string.note_you_are_trying_to_change_the_account_associated_with_the_loan,
                loan.name
            )
        }

        val loanCaption =
            if (isLoanRecord) {
                context.getString(
                    R.string.this_transaction_is_associated_with_loan_record,
                    loan.name
                )
            } else {
                context.getString(
                    R.string.this_transaction_is_associated_with_loan,
                    loan.name
                )
            }

        return EditTransactionDisplayLoan(
            isLoan = true,
            isLoanRecord = isLoanRecord,
            loanCaption = loanCaption,
            loanWarningDescription = loanWarningDescription
        )
    }
}
