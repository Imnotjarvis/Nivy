package com.ivy.home.usecase

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HistoryWithDateDividersUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionsWithDateDividersUseCase: TransactionsWithDateDividersUseCase,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun history(range: ClosedTimeRange, baseCurrency: String): ImmutableList<TransactionHistoryItem> =
        withContext(dispatchers.io) {
            transactionsWithDateDividersUseCase.withDateDividers(
                baseCurrency = baseCurrency,
                transactions = transactionRepository.findAllBetween(range.from, range.to)
            ).toImmutableList()
        }
}
