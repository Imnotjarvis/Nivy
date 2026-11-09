package com.nivy.piechart

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Account

/** What [account] paid out, or took in, of the chart's type, in the base currency. */
@Immutable
data class AccountAmount(
    val account: Account,
    val amount: Double,
)
