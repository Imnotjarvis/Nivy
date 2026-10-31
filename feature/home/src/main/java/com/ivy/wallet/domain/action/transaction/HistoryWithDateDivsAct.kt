package com.ivy.wallet.domain.action.transaction

import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryWithDateDivsAct @Inject constructor(
    private val historyTrnsAct: HistoryTrnsAct,
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct
) {
    suspend operator fun invoke(input: Input): ImmutableList<TransactionHistoryItem> =
        withContext(Dispatchers.IO) {
            trnsWithDateDivsAct(
                TrnsWithDateDivsAct.Input(
                    baseCurrency = input.baseCurrency,
                    transactions = historyTrnsAct(input.range)
                )
            ).toImmutableList()
        }

    data class Input(
        val range: ClosedTimeRange,
        val baseCurrency: String
    )
}
