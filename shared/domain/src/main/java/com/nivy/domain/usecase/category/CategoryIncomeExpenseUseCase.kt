package com.nivy.domain.usecase.category

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.Transaction
import com.nivy.data.temp.migration.getAccountId
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CategoryIncomeExpenseUseCase @Inject constructor(
    private val transactionsIncomeExpenseUseCase: TransactionsIncomeExpenseUseCase,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun incomeExpense(
        transactions: List<Transaction>,
        accountFilterList: List<Account>,
        category: Category?,
        baseCurrency: String,
    ): IncomeExpenseTransferPair = withContext(dispatchers.io) {
        val accountFilterSet = accountFilterList.map { it.id.value }.toHashSet()
        val categoryTransactions = transactions
            .filter { it.category?.value == category?.id?.value }
            .filter { accountFilterSet.isEmpty() || accountFilterSet.contains(it.getAccountId()) }
        transactionsIncomeExpenseUseCase.incomeExpense(
            transactions = categoryTransactions,
            baseCurrency = baseCurrency,
            accounts = accountFilterList
        )
    }
}
