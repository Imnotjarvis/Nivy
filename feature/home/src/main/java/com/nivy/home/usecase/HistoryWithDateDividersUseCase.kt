package com.nivy.home.usecase

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.transaction.TransactionsWithDateDividersUseCase
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
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
