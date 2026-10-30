package com.ivy.wallet.domain.action.viewmodel.home

import com.ivy.data.model.Transaction
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.transaction.isUpcoming
import javax.inject.Inject

class UpcomingAct @Inject constructor(
    private val dueTrnsInfoAct: DueTrnsInfoAct
) {
    suspend operator fun invoke(input: Input): Output {
        val info = dueTrnsInfoAct(
            DueTrnsInfoAct.Input(
                range = input.range,
                baseCurrency = input.baseCurrency,
                dueFilter = ::isUpcoming
            )
        )
        return Output(
            upcoming = info.dueIncomeExpense,
            upcomingTrns = info.dueTrns
        )
    }

    data class Input(
        val range: ClosedTimeRange,
        val baseCurrency: String
    )

    data class Output(
        val upcoming: IncomeExpensePair,
        val upcomingTrns: List<Transaction>
    )
}
