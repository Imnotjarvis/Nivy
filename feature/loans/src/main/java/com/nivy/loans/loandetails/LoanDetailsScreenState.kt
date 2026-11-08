package com.nivy.loans.loandetails

import com.nivy.data.model.Account
import com.nivy.data.model.Loan
import com.nivy.loans.LoanProgress
import com.nivy.loans.loan.data.DisplayLoanRecord
import com.nivy.loans.LoanModalData
import java.time.Instant
import kotlinx.collections.immutable.ImmutableList

data class LoanDetailsScreenState(
    val baseCurrency: String,
    val loan: Loan?,
    val displayLoanRecords: ImmutableList<DisplayLoanRecord>,
    val progress: LoanProgress,
    val accounts: ImmutableList<Account>,
    val selectedLoanAccount: Account?,
    val createLoanTransaction: Boolean,
    val loanModalData: LoanModalData?,
    val loanRecordModalData: LoanRecordModalData?,
    val waitModalVisible: Boolean,
    val isDeleteModalVisible: Boolean,
    val dateTime: Instant
)
