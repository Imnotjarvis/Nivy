package com.nivy.loans.loan

import com.nivy.data.model.Account
import com.nivy.loans.loan.data.DisplayLoan
import com.nivy.loans.LoanModalData
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant

data class LoanScreenState(
    val completedLoans: ImmutableList<DisplayLoan>,
    val pendingLoans: ImmutableList<DisplayLoan>,
    val accounts: ImmutableList<Account>,
    val loanModalData: LoanModalData?,
    val reorderModalVisible: Boolean,
    val totalOweAmount: String,
    val totalOwedAmount: String,
    val dateTime: Instant,
    val selectedTab: LoanTab
)
