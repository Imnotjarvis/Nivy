package com.nivy.wallet.data

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Transaction
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class DueSection(
    val trns: ImmutableList<Transaction>,
    val expanded: Boolean,
    val stats: IncomeExpensePair
)
