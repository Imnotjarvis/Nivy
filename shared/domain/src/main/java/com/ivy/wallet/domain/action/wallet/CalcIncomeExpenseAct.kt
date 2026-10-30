package com.ivy.wallet.domain.action.wallet

import arrow.core.nonEmptyListOf
import arrow.core.toOption
import com.ivy.data.model.Account
import com.ivy.wallet.domain.action.account.AccTrnsAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.account.filterExcluded
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.legacy.domain.pure.transaction.AccountValueFunctions
import com.ivy.wallet.domain.pure.transaction.foldTransactions
import com.ivy.wallet.domain.pure.util.orZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class CalcIncomeExpenseAct @Inject constructor(
    private val accTrnsAct: AccTrnsAct,
    private val exchangeAct: ExchangeAct
) {
    suspend operator fun invoke(input: Input): IncomeExpensePair = withContext(Dispatchers.IO) {
        val statsList = filterExcluded(input.accounts).map { acc ->
            val trns = accTrnsAct(
                AccTrnsAct.Input(
                    accountId = acc.id.value,
                    range = input.range
                )
            )
            Timber.i("acc: $acc, trns = ${trns.size}")
            val stats = foldTransactions(
                transactions = trns,
                valueFunctions = nonEmptyListOf(
                    AccountValueFunctions::income,
                    AccountValueFunctions::expense
                ),
                arg = acc.id.value
            )
            Timber.i("acc_stats: $acc - $stats")
            stats.map {
                exchangeAct(
                    ExchangeAct.Input(
                        data = ExchangeData(
                            baseCurrency = input.baseCurrency,
                            fromCurrency = acc.asset.code.toOption()
                        ),
                        amount = it
                    ),
                ).orZero()
            }
        }
        IncomeExpensePair(
            income = statsList.sumOf { it[0] },
            expense = statsList.sumOf { it[1] }
        )
    }

    data class Input(
        val baseCurrency: String,
        val accounts: List<Account>,
        val range: ClosedTimeRange,
    )
}
