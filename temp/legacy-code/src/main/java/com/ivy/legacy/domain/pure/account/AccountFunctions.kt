package com.ivy.wallet.domain.pure.account

import com.ivy.data.model.Account

fun filterExcluded(accounts: List<Account>): List<Account> =
    accounts.filter { it.includeInBalance }

fun accountCurrency(account: Account): String = account.asset.code
