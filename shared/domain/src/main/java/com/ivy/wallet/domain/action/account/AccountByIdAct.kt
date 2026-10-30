package com.ivy.wallet.domain.action.account

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import java.util.UUID
import javax.inject.Inject

class AccountByIdAct @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(accountId: UUID): Account? =
        accountRepository.findById(AccountId(accountId))
}
