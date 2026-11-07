package com.nivy.wallet.domain.pure.account

import com.nivy.data.model.Account

fun filterExcluded(accounts: List<Account>): List<Account> =
    accounts.filter { it.includeInBalance }
