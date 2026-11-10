package com.nivy.domain.usecase.wallet

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.data.model.primitive.AssetCode
import com.nivy.domain.usecase.account.AccountTransactionsUseCase
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.account.filterExcluded
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import com.nivy.wallet.domain.pure.transaction.AccountValueFunctions
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletIncomeExpenseUseCase @Inject constructor(
    private val accountTransactionsUseCase: AccountTransactionsUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val dispatchers: DispatchersProvider,
) {
    /** What came in and went out of the [accounts] in the balance in [range], in the [baseCurrency]. */
    suspend fun incomeExpense(
        baseCurrency: String,
        accounts: List<Account>,
        range: ClosedTimeRange,
    ): IncomeExpensePair = withContext(dispatchers.io) {
        val rates = BaseCurrencyRates(exchangeUseCase, AssetCode.unsafe(baseCurrency))
        var income = 0.0
        var expense = 0.0
        filterExcluded(accounts).forEach { account ->
            val id = account.id.value
            val transactions = accountTransactionsUseCase.transactions(accountId = id, range = range)
            val accountIncome = transactions.sumOf { AccountValueFunctions.income(it, id).toDouble() }
            val accountExpense = transactions.sumOf { AccountValueFunctions.expense(it, id).toDouble() }
            income += rates.inBase(accountIncome, account.asset)
            expense += rates.inBase(accountExpense, account.asset)
        }
        IncomeExpensePair(income = income.toBigDecimal(), expense = expense.toBigDecimal())
    }
}
