package com.nivy.domain.usecase.transaction

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionsIncomeExpenseUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
    private val dispatchers: DispatchersProvider,
) {
    /**
     * The [transactions]' income and expenses in the [baseCurrency], and what their transfers
     * brought into the [accounts] and took out of them.
     */
    suspend fun incomeExpense(
        transactions: List<Transaction>,
        baseCurrency: String,
        accounts: List<Account>,
    ): IncomeExpenseTransferPair = withContext(dispatchers.io) {
        val rates = BaseCurrencyRates(exchangeUseCase, AssetCode.unsafe(baseCurrency))
        val ids = accounts.map { it.id }.toSet()
        var income = 0.0
        var expense = 0.0
        var transferIncome = 0.0
        var transferExpense = 0.0
        transactions.forEach { transaction ->
            when (transaction) {
                is Income -> income += rates.inBase(transaction.value)
                is Expense -> expense += rates.inBase(transaction.value)
                is Transfer -> {
                    if (transaction.toAccount in ids) transferIncome += rates.inBase(transaction.toValue)
                    if (transaction.fromAccount in ids) transferExpense += rates.inBase(transaction.fromValue)
                }
            }
        }
        IncomeExpenseTransferPair(
            income = income.toBigDecimal(),
            expense = expense.toBigDecimal(),
            transferIncome = transferIncome.toBigDecimal(),
            transferExpense = transferExpense.toBigDecimal(),
        )
    }
}
