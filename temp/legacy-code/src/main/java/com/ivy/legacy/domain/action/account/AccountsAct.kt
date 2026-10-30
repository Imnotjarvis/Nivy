package com.ivy.wallet.domain.action.account

import com.ivy.data.db.dao.read.AccountDao
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountsAct @Inject constructor(
    private val accountDao: AccountDao
) {
    suspend operator fun invoke(): ImmutableList<Account> = withContext(Dispatchers.IO) {
        accountDao.findAll().map { it.toLegacyDomain() }.toImmutableList()
    }
}
