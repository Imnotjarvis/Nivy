package com.ivy.wallet.domain.action.account

import arrow.core.nonEmptyListOf
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.wallet.domain.pure.transaction.AccountValueFunctions
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.transaction.foldTransactions
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalcAccBalanceAct @Inject constructor(
    private val accTrnsAct: AccTrnsAct,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(input: Input): Output = withContext(Dispatchers.IO) {
        val accTrns = accTrnsAct(
            AccTrnsAct.Input(
                accountId = input.account.id.value,
                range = input.range ?: ClosedTimeRange.allTimeIvy(timeProvider)
            )
        )
        val balance = foldTransactions(
            transactions = accTrns,
            arg = input.account.id.value,
            valueFunctions = nonEmptyListOf(AccountValueFunctions::balance)
        ).head
        Output(account = input.account, balance = balance)
    }

    @Suppress("DataClassDefaultValues")
    data class Input(
        val account: Account,
        val range: ClosedTimeRange? = null
    )

    data class Output(
        val account: Account,
        val balance: BigDecimal,
    )
}
