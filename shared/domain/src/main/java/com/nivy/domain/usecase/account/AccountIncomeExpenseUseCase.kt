package com.nivy.domain.usecase.account

import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.model.Account
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
        account: Account,
        range: ClosedTimeRange? = null,
        includeTransfersInCalc: Boolean = false,
    ): IncomeExpensePair = withContext(dispatchers.io) {
        val transactions = accountTransactionsUseCase.transactions(
            accountId = account.id.value,
            range = range ?: ClosedTimeRange.allTimeNivy(timeProvider)
        )
        incomeExpenseOf(account, transactions, includeTransfersInCalc)
    }

    /**
     * What [transactions], as [AccountTransactionsUseCase] reads them, brought into and took out of
     * [account], in its own currency; transfers too when [includeTransfersInCalc].
     */
    fun incomeExpenseOf(
        account: Account,
        transactions: List<Transaction>,
        includeTransfersInCalc: Boolean,
    ): IncomeExpensePair {
        val id = account.id.value
        fun sum(value: (Transaction, UUID) -> BigDecimal): BigDecimal =
            transactions.fold(BigDecimal.ZERO) { total, transaction -> total + value(transaction, id) }

        val transfers = includeTransfersInCalc
        return IncomeExpensePair(
            income = sum(AccountValueFunctions::income) +
                if (transfers) sum(AccountValueFunctions::transferIncome) else BigDecimal.ZERO,
            expense = sum(AccountValueFunctions::expense) +
                if (transfers) sum(AccountValueFunctions::transferExpense) else BigDecimal.ZERO,
        )
    }
}
