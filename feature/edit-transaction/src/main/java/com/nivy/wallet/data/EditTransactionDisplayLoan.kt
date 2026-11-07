package com.nivy.wallet.data

import androidx.compose.runtime.Immutable

@Immutable
data class EditTransactionDisplayLoan(
    val isLoan: Boolean = false,
    val isLoanRecord: Boolean = false,
    val loanCaption: String? = null,
)
