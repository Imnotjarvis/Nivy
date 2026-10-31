package com.ivy.wallet.domain.logic.model

import com.ivy.data.model.LoanRecord

data class EditLoanRecordData(
    val newLoanRecord: LoanRecord,
    val originalLoanRecord: LoanRecord,
    val createLoanRecordTransaction: Boolean = false,
    val reCalculateLoanAmount: Boolean = false
)
