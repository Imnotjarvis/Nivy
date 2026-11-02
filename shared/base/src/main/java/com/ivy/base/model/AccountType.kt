package com.ivy.base.model

import androidx.annotation.Keep

/** What an account is. Only [CREDIT_CARD] changes behavior: see Account.chargeDay. */
@Keep
enum class AccountType {
    CASH, BANK, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER
}
