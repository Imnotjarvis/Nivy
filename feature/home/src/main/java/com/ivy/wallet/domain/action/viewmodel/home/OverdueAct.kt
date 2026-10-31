package com.ivy.wallet.domain.action.viewmodel.home

import com.ivy.wallet.utils.ivyMinTime
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.transaction.isOverdue
import java.time.Instant
import javax.inject.Inject

class OverdueAct @Inject constructor(
    private val dueTrnsInfoAct: DueTrnsInfoAct
) {
    suspend operator fun invoke(input: Input): Output {
        val info = dueTrnsInfoAct(
            DueTrnsInfoAct.Input(
                range = ClosedTimeRange(
                    from = ivyMinTime(),
                    to = input.toRange
                ),
                baseCurrency = input.baseCurrency,
                dueFilter = ::isOverdue
            )
        )
        return Output(
            overdue = info.dueIncomeExpense,
            overdueTrns = info.dueTrns
        )
    }

    data class Input(
        val toRange: Instant,
        val baseCurrency: String
    )

    data class Output(
        val overdue: IncomeExpensePair,
        val overdueTrns: List<com.ivy.data.model.Transaction>
    )
}
