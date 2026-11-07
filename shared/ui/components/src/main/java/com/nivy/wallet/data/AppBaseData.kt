package com.nivy.wallet.data

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class AppBaseData(
    val baseCurrency: String,
    val accounts: ImmutableList<Account>,
    val categories: ImmutableList<Category>
)
