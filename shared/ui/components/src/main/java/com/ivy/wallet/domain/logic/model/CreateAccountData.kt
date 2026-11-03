package com.ivy.wallet.domain.logic.model

import androidx.compose.ui.graphics.Color
import com.ivy.base.model.AccountType

data class CreateAccountData(
    val name: String,
    val currency: String,
    val color: Color,
    val icon: String?,
    val balance: Double,
    val includeBalance: Boolean = true,
    val type: AccountType = AccountType.OTHER,
    // Only for a credit card.
    val creditLimit: Double? = null,
    val chargeDay: Int? = null,
    val statementClosingDay: Int? = null,
)
