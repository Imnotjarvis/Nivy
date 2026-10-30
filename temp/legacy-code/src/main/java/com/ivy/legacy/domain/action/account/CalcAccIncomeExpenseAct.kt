package com.ivy.wallet.domain.action.account

import arrow.core.nonEmptyListOf
import com.ivy.base.time.TimeProvider
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.legacy.domain.pure.transaction.AccountValueFunctions
import com.ivy.wallet.domain.pure.transaction.foldTransactions
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalcAccIncomeExpenseAct @Inject constructor(
    private val accTrnsAct: AccTrnsAct,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(input: Input): Output = withContext(Dispatchers.IO) {
        val accTrns = accTrnsAct(
            AccTrnsAct.Input(
                accountId = input.account.id.value,
                range = input.range ?: ClosedTimeRange.allTimeIvy(timeProvider)
            )
        )
        val values = foldTransactions(
            transactions = accTrns,
            arg = input.account.id.value,
            valueFunctions = nonEmptyListOf(
                AccountValueFunctions::income,
                AccountValueFunctions::expense,
                AccountValueFunctions::transferIncome,
                AccountValueFunctions::transferExpense
            )
        )
        val transfers = input.includeTransfersInCalc
        Output(
            account = input.account,
            incomeExpensePair = IncomeExpensePair(
                income = values[0] + if (transfers) values[2] else BigDecimal.ZERO,
                expense = values[1] + if (transfers) values[3] else BigDecimal.ZERO
            )
        )
    }

    @Suppress("DataClassDefaultValues")
    data class Input(
        val account: com.ivy.data.model.Account,
        val range: ClosedTimeRange? = null,
        val includeTransfersInCalc: Boolean = false
    )

    data class Output(
        val account: com.ivy.data.model.Account,
        val incomeExpensePair: IncomeExpensePair
    )
}
