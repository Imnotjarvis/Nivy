package com.ivy.wallet.domain.action.viewmodel.home

import com.ivy.data.db.dao.read.TransactionDao
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HasTrnsAct @Inject constructor(
    private val transactionDao: TransactionDao
) {
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        transactionDao.hasTransactions()
    }
}
