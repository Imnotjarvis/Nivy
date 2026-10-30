package com.ivy.wallet.domain.action.account

import com.ivy.data.db.dao.read.AccountDao
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountByIdAct @Inject constructor(
    private val accountDao: AccountDao
) {
    suspend operator fun invoke(accountId: UUID): Account? = withContext(Dispatchers.IO) {
        accountDao.findById(accountId)?.toLegacyDomain()
    }
}
