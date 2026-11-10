package com.nivy.domain.usecase.account

import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Transaction
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.transaction.AccountValueFunctions
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

class AccountIncomeExpenseUseCase @Inject constructor(
    private val accountTransactionsUseCase: AccountTransactionsUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {
    /**
     * What came into and went out of [account] in [range], all time without one, in its own
     * currency; transfers too when [includeTransfersInCalc].
     */
    suspend fun incomeExpense(
        account: com.nivy.data.model.Account,
        range: ClosedTimeRange? = null,
        includeTransfersInCalc: Boolean = false,
    ): AccountIncomeExpense = withContext(dispatchers.io) {
        val id = account.id.value
        val transactions = accountTransactionsUseCase.transactions(
            accountId = id,
            range = range ?: ClosedTimeRange.allTimeNivy(timeProvider)
        )
        fun sum(value: (Transaction, UUID) -> BigDecimal): BigDecimal =
            transactions.fold(BigDecimal.ZERO) { total, transaction -> total + value(transaction, id) }

        val transfers = includeTransfersInCalc
        AccountIncomeExpense(
            account = account,
            incomeExpensePair = IncomeExpensePair(
                income = sum(AccountValueFunctions::income) +
                    if (transfers) sum(AccountValueFunctions::transferIncome) else BigDecimal.ZERO,
                expense = sum(AccountValueFunctions::expense) +
                    if (transfers) sum(AccountValueFunctions::transferExpense) else BigDecimal.ZERO,
            )
        )
    }
}

data class AccountIncomeExpense(
    val account: com.nivy.data.model.Account,
    val incomeExpensePair: IncomeExpensePair
)
