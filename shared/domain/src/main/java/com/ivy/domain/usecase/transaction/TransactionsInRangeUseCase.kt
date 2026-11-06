package com.ivy.domain.usecase.transaction

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.Transaction
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getToAccount
import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** The transactions in a range that leave or reach one of the filter's accounts. */
class TransactionsInRangeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun transactions(
        range: ClosedTimeRange,
        accountIdFilterSet: Set<UUID>,
    ): List<Transaction> = withContext(dispatchers.io) {
        transactionRepository.findAllBetween(range.from, range.to)
            .filter {
                accountIdFilterSet.contains(it.getFromAccount().value) ||
                    accountIdFilterSet.contains(it.getToAccount()?.value)
            }
    }
}
