package com.ivy.home.usecase

import com.ivy.domain.usecase.planned.DueTransactionsInfoUseCase
import com.ivy.wallet.utils.ivyMinTime
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.transaction.isOverdue
import java.time.Instant
import javax.inject.Inject

class OverdueTransactionsUseCase @Inject constructor(
    private val dueTransactionsInfoUseCase: DueTransactionsInfoUseCase
) {
    suspend fun overdue(toRange: Instant, baseCurrency: String): OverdueTransactions {
        val info = dueTransactionsInfoUseCase.info(
            range = ClosedTimeRange(
                from = ivyMinTime(),
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
    val overdueTrns: List<com.ivy.data.model.Transaction>
)
