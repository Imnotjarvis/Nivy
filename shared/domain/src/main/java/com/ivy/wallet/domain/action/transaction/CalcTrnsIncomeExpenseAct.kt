package com.ivy.wallet.domain.action.transaction

import arrow.core.nonEmptyListOf
import com.ivy.data.model.Account
import com.ivy.data.model.Transaction
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import com.ivy.wallet.domain.pure.transaction.WalletValueFunctions
import com.ivy.wallet.domain.pure.transaction.foldTransactionsSuspend
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalcTrnsIncomeExpenseAct @Inject constructor(
    private val exchangeAct: ExchangeAct
) {
    suspend operator fun invoke(input: Input): IncomeExpenseTransferPair = withContext(Dispatchers.IO) {
        val values = foldTransactionsSuspend(
            transactions = input.transactions,
            valueFunctions = nonEmptyListOf(
                WalletValueFunctions::income,
                WalletValueFunctions::expense,
                WalletValueFunctions::transferIncome,
                WalletValueFunctions::transferExpenses
            ),
            arg = WalletValueFunctions.Argument(
                accounts = input.accounts,
                baseCurrency = input.baseCurrency,
                exchange = exchangeAct::convert
            )
        )
        IncomeExpenseTransferPair(
            income = values[0],
            expense = values[1],
            transferIncome = values[2],
            transferExpense = values[3]
        )
    }

    data class Input(
        val transactions: List<Transaction>,
        val baseCurrency: String,
        val accounts: List<Account>
    )
}
