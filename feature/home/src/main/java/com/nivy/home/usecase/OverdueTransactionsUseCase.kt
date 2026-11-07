package com.nivy.home.usecase

import com.nivy.domain.usecase.planned.DueTransactionsInfoUseCase
import com.nivy.wallet.utils.nivyMinTime
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.transaction.isOverdue
import java.time.Instant
import javax.inject.Inject

class OverdueTransactionsUseCase @Inject constructor(
    private val dueTransactionsInfoUseCase: DueTransactionsInfoUseCase
) {
    suspend fun overdue(toRange: Instant, baseCurrency: String): OverdueTransactions {
        val info = dueTransactionsInfoUseCase.info(
            range = ClosedTimeRange(
                from = nivyMinTime(),
                to = toRange
            ),
            baseCurrency = baseCurrency,
            dueFilter = ::isOverdue
        )
        return OverdueTransactions(
            overdue = info.dueIncomeExpense,
            overdueTrns = info.dueTrns
        )
    }
}

data class OverdueTransactions(
    val overdue: IncomeExpensePair,
    val overdueTrns: List<com.nivy.data.model.Transaction>
)
