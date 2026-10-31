package com.ivy.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.ivy.base.model.IntervalType
import com.ivy.ui.R

@Composable
@ReadOnlyComposable
fun IntervalType.forDisplay(intervalN: Int): String {
    val plural = intervalN > 1 || intervalN == 0
    return when (this) {
        IntervalType.DAY -> if (plural) stringResource(R.string.days) else stringResource(R.string.day)
        IntervalType.WEEK -> if (plural) stringResource(R.string.weeks) else stringResource(R.string.week)
        IntervalType.MONTH -> if (plural) stringResource(R.string.months) else stringResource(R.string.month)
        IntervalType.YEAR -> if (plural) stringResource(R.string.years) else stringResource(R.string.year)
    }
}
