package com.ivy.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanType
import com.ivy.ui.R

@Composable
@ReadOnlyComposable
fun Loan.humanReadableType(): String {
    return if (type == LoanType.BORROW) {
        stringResource(R.string.borrowed_uppercase)
    } else {
        stringResource(R.string.lent_uppercase)
    }
}
