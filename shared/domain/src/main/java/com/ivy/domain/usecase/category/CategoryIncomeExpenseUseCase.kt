package com.ivy.domain.usecase.category

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.Transaction
import com.ivy.data.temp.migration.getAccountId
import com.ivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
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
