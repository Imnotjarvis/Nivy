package com.ivy.wallet.domain.action.transaction

import com.ivy.data.repository.TransactionRepository
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryWithDateDivsAct @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct
) {
    suspend operator fun invoke(input: Input): ImmutableList<TransactionHistoryItem> =
        withContext(Dispatchers.IO) {
            trnsWithDateDivsAct(
                TrnsWithDateDivsAct.Input(
                    baseCurrency = input.baseCurrency,
                    transactions = transactionRepository.findAllBetween(input.range.from, input.range.to)
                )
            ).toImmutableList()
        }

    data class Input(
        val range: ClosedTimeRange,
        val baseCurrency: String
    )
}
