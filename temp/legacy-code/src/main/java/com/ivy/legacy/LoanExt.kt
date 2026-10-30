package com.ivy.legacy

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.data.model.LoanType
import com.ivy.legacy.datamodel.Loan
import com.ivy.ui.R

@Composable
fun Loan.humanReadableType(): String {
    return if (type == LoanType.BORROW) {
        stringResource(R.string.borrowed_uppercase)
    } else {
        stringResource(R.string.lent_uppercase)
    }
}
