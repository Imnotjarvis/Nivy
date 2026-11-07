package com.nivy.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nivy.data.model.Loan
import com.nivy.base.model.LoanType
import com.nivy.ui.R

@Composable
@ReadOnlyComposable
fun Loan.humanReadableType(): String {
    return if (type == LoanType.BORROW) {
        stringResource(R.string.borrowed_uppercase)
    } else {
        stringResource(R.string.lent_uppercase)
    }
}
