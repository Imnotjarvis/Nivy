package com.nivy.accounts

import com.nivy.wallet.data.model.AccountData

/**
 * What the accounts' balances add up to in [baseCurrency], each converted from its own currency,
 * one with no rate counting as nothing: the accounts in the balance, or every one [withExcluded].
 * From the balances already computed, rather than from every transaction again.
 */
internal fun List<AccountData>.totalIn(baseCurrency: String, withExcluded: Boolean): Double =
    filter { withExcluded || it.account.includeInBalance }
        .sumOf { data ->
            val inBaseCurrency = if (data.account.asset.code == baseCurrency) {
                data.balance
            } else {
                data.balanceBaseCurrency ?: 0.0
            }
            inBaseCurrency.toBigDecimal()
        }
        .toDouble()
