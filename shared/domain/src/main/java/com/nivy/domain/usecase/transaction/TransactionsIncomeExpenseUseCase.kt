package com.nivy.domain.usecase.transaction

import arrow.core.nonEmptyListOf
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Transaction
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import com.nivy.wallet.domain.pure.transaction.WalletValueFunctions
import com.nivy.wallet.domain.pure.transaction.foldTransactionsSuspend
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
