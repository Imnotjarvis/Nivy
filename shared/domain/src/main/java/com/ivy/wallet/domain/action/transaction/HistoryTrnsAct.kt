package com.ivy.wallet.domain.action.transaction

import com.ivy.data.model.Transaction
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryTrnsAct @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(range: ClosedTimeRange): List<Transaction> = withContext(Dispatchers.IO) {
        transactionRepository.findAllBetween(
            startDate = range.from,
            endDate = range.to
        )
    }
}
