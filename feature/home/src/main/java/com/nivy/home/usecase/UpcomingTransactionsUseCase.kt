package com.nivy.home.usecase

import com.nivy.data.model.Transaction
import com.nivy.domain.usecase.planned.DueTransactionsInfoUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.transaction.isUpcoming
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
