package com.nivy.wallet.domain.logic.model

import androidx.compose.ui.graphics.Color
import com.nivy.base.model.AccountType
import com.nivy.data.model.CreditCard

data class CreateAccountData(
    val name: String,
    val currency: String,
    val color: Color,
    val icon: String?,
    val balance: Double,
    val includeBalance: Boolean = true,
    val type: AccountType = AccountType.OTHER,
    // Its credit cards: they become the new account's when it's created.
    val creditCards: List<CreditCard> = emptyList(),
)
