package com.nivy.wallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nivy.base.model.AccountType
import com.nivy.ui.R

@Composable
@ReadOnlyComposable
fun AccountType.displayName(): String = stringResource(
    when (this) {
        AccountType.CASH -> R.string.account_type_cash
        AccountType.BANK -> R.string.account_type_bank
        AccountType.CREDIT_CARD -> R.string.account_type_credit_card
        AccountType.SAVINGS -> R.string.account_type_savings
        AccountType.INVESTMENT -> R.string.account_type_investment
        AccountType.OTHER -> R.string.account_type_other
    }
)

/** One of the account icons for the type; null for the default account icon. */
fun AccountType.iconName(): String? = when (this) {
    AccountType.CASH -> "cash"
    AccountType.BANK -> "bank"
    AccountType.SAVINGS -> "safe"
    AccountType.INVESTMENT -> "stats"
    AccountType.CREDIT_CARD, AccountType.OTHER -> null
}
