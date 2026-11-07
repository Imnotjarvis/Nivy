package com.nivy.base.model

import androidx.annotation.Keep

/** What an account is. Only [CREDIT_CARD] changes behavior: its new expenses are paid with its card. */
@Keep
enum class AccountType {
    CASH, BANK, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER
}
