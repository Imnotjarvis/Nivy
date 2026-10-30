package com.ivy.wallet.domain.action.loan

import com.ivy.data.db.dao.read.LoanDao
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoansAct @Inject constructor(
    private val loanDao: LoanDao
) {
    suspend operator fun invoke(): ImmutableList<Loan> = withContext(Dispatchers.IO) {
        loanDao.findAll().map { it.toLegacyDomain() }.toImmutableList()
    }
}
