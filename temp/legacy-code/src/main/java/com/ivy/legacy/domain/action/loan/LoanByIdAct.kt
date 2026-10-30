package com.ivy.wallet.domain.action.loan

import com.ivy.data.db.dao.read.LoanDao
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoanByIdAct @Inject constructor(
    private val loanDao: LoanDao
) {
    suspend operator fun invoke(loanId: UUID): Loan? = withContext(Dispatchers.IO) {
        loanDao.findById(loanId)?.toLegacyDomain()
    }
}
