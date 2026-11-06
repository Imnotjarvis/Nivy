package com.ivy.home.usecase

import com.ivy.data.model.Transaction
import com.ivy.domain.usecase.planned.DueTransactionsInfoUseCase
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.transaction.isUpcoming
import javax.inject.Inject

class UpcomingTransactionsUseCase @Inject constructor(
    private val dueTransactionsInfoUseCase: DueTransactionsInfoUseCase
) {
    suspend fun upcoming(range: ClosedTimeRange, baseCurrency: String): UpcomingTransactions {
        val info = dueTransactionsInfoUseCase.info(
            range = range,
            baseCurrency = baseCurrency,
            dueFilter = ::isUpcoming
        )
        return UpcomingTransactions(
            upcoming = info.dueIncomeExpense,
            upcomingTrns = info.dueTrns
        )
    }
}

data class UpcomingTransactions(
    val upcoming: IncomeExpensePair,
    val upcomingTrns: List<Transaction>
)
