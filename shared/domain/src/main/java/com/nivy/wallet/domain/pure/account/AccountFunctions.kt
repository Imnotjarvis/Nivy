package com.nivy.wallet.domain.pure.account

import com.nivy.data.model.Account
import java.util.UUID

fun filterExcluded(accounts: List<Account>): List<Account> =
    accounts.filter { it.includeInBalance }

/**
 * The account a new transaction or loan starts on, of those that can be picked: the [lastChosen]
 * one while it's among them, else the first; null without any.
 */
fun List<Account>.startingAccount(lastChosen: UUID?): Account? =
    firstOrNull { it.id.value == lastChosen } ?: firstOrNull()
