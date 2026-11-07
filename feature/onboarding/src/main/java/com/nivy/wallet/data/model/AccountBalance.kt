package com.nivy.wallet.data.model

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Account

@Immutable
data class AccountBalance(
    val account: Account,
    val balance: Double
)
