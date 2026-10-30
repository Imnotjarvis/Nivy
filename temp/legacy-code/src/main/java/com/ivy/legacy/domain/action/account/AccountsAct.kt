package com.ivy.wallet.domain.action.account

import com.ivy.data.model.Account
import com.ivy.data.repository.AccountRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class AccountsAct @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /** Sorted by [Account.orderNum]. */
    suspend operator fun invoke(): ImmutableList<Account> =
        accountRepository.findAll().toImmutableList()
}
