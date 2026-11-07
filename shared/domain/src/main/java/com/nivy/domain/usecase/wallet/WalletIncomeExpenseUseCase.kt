package com.nivy.domain.usecase.wallet

import arrow.core.nonEmptyListOf
import arrow.core.toOption
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.account.filterExcluded
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import com.nivy.wallet.domain.pure.transaction.AccountValueFunctions
import com.nivy.wallet.domain.pure.transaction.foldTransactions
import com.nivy.wallet.domain.pure.util.orZero
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletIncomeExpenseUseCase @Inject constructor(
    private val accountTransactionsUseCase: AccountTransactionsUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun incomeExpense(
        baseCurrency: String,
        accounts: List<Account>,
        range: ClosedTimeRange,
    ): IncomeExpensePair = withContext(dispatchers.io) {
        val statsList = filterExcluded(accounts).map { acc ->
            val trns = accountTransactionsUseCase.transactions(
                accountId = acc.id.value,
                range = range
            )
            val stats = foldTransactions(
                transactions = trns,
                valueFunctions = nonEmptyListOf(
                    AccountValueFunctions::income,
                    AccountValueFunctions::expense
                ),
                arg = acc.id.value
            )
            stats.map {
                exchangeUseCase.exchange(
                    data = ExchangeData(
                        baseCurrency = baseCurrency,
                        fromCurrency = acc.asset.code.toOption()
                    ),
                    amount = it
                ).orZero()
            }
        }
        IncomeExpensePair(
            income = statsList.sumOf { it[0] },
            expense = statsList.sumOf { it[1] }
        )
    }
}
