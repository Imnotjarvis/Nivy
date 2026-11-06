package com.ivy.domain.usecase.transaction

import arrow.core.nonEmptyListOf
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Transaction
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import com.ivy.wallet.domain.pure.transaction.WalletValueFunctions
import com.ivy.wallet.domain.pure.transaction.foldTransactionsSuspend
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionsIncomeExpenseUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun incomeExpense(
        transactions: List<Transaction>,
        baseCurrency: String,
        accounts: List<Account>,
    ): IncomeExpenseTransferPair = withContext(dispatchers.io) {
        val values = foldTransactionsSuspend(
            transactions = transactions,
            valueFunctions = nonEmptyListOf(
                WalletValueFunctions::income,
                WalletValueFunctions::expense,
                WalletValueFunctions::transferIncome,
                WalletValueFunctions::transferExpenses
            ),
            arg = WalletValueFunctions.Argument(
                accounts = accounts,
                baseCurrency = baseCurrency,
                exchange = exchangeUseCase::exchange
            )
        )
        IncomeExpenseTransferPair(
            income = values[0],
            expense = values[1],
            transferIncome = values[2],
            transferExpense = values[3]
        )
    }
}
