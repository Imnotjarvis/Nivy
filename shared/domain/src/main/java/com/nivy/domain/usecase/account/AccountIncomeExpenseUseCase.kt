package com.nivy.domain.usecase.account

import arrow.core.nonEmptyListOf
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.transaction.AccountValueFunctions
import com.nivy.wallet.domain.pure.transaction.foldTransactions
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class AccountIncomeExpenseUseCase @Inject constructor(
    private val accountTransactionsUseCase: AccountTransactionsUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun incomeExpense(
        account: com.nivy.data.model.Account,
        range: ClosedTimeRange? = null,
        includeTransfersInCalc: Boolean = false,
    ): AccountIncomeExpense = withContext(dispatchers.io) {
        val accTrns = accountTransactionsUseCase.transactions(
            accountId = account.id.value,
            range = range ?: ClosedTimeRange.allTimeNivy(timeProvider)
        )
        val values = foldTransactions(
            transactions = accTrns,
            arg = account.id.value,
            valueFunctions = nonEmptyListOf(
                AccountValueFunctions::income,
                AccountValueFunctions::expense,
                AccountValueFunctions::transferIncome,
                AccountValueFunctions::transferExpense
            )
        )
        val transfers = includeTransfersInCalc
        AccountIncomeExpense(
            account = account,
            incomeExpensePair = IncomeExpensePair(
                income = values[0] + if (transfers) values[2] else BigDecimal.ZERO,
                expense = values[1] + if (transfers) values[3] else BigDecimal.ZERO
            )
        )
    }
}

data class AccountIncomeExpense(
    val account: com.nivy.data.model.Account,
    val incomeExpensePair: IncomeExpensePair
)
