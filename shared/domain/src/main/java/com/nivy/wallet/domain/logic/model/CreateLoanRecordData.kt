package com.nivy.wallet.domain.logic.model

import com.nivy.base.model.LoanRecordType
import com.nivy.data.model.Account
import java.time.Instant

data class CreateLoanRecordData(
    val note: String?,
    val amount: Double,
    val dateTime: Instant,
    val interest: Boolean = false,
    val account: Account? = null,
    val createLoanRecordTransaction: Boolean = false,
    val loanRecordType: LoanRecordType
)
