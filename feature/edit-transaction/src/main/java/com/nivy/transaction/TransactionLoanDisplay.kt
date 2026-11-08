package com.nivy.transaction

import android.content.Context
import com.nivy.data.db.dao.read.LoanDao
import com.nivy.ui.R
import com.nivy.wallet.data.EditTransactionDisplayLoan
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

        val loan = loanDao.findById(trans.loanId) ?: return EditTransactionDisplayLoan()
        val isLoanRecord = trans.loanRecordId != null

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
        )
    }
}
