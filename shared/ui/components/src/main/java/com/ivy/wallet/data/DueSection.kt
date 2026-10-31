package com.ivy.wallet.data

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Transaction
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class DueSection(
    val trns: ImmutableList<Transaction>,
    val expanded: Boolean,
    val stats: IncomeExpensePair
)
