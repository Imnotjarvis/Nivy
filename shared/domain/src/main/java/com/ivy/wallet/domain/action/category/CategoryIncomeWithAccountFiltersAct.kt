package com.ivy.wallet.domain.action.category

import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.Transaction
import com.ivy.data.temp.migration.getAccountId
import com.ivy.wallet.domain.action.transaction.CalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryIncomeWithAccountFiltersAct @Inject constructor(
    private val calcTrnsIncomeExpenseAct: CalcTrnsIncomeExpenseAct
) {
    suspend operator fun invoke(input: Input): IncomeExpenseTransferPair = withContext(Dispatchers.IO) {
        val accountFilterSet = input.accountFilterList.map { it.id.value }.toHashSet()
        val transactions = input.transactions
            .filter { it.category?.value == input.category?.id?.value }
            .filter { accountFilterSet.isEmpty() || accountFilterSet.contains(it.getAccountId()) }
        calcTrnsIncomeExpenseAct(
            CalcTrnsIncomeExpenseAct.Input(
                transactions = transactions,
                baseCurrency = input.baseCurrency,
                accounts = input.accountFilterList
            )
        )
    }

    data class Input(
        val transactions: List<Transaction>,
        val accountFilterList: List<Account>,
        val category: Category?,
        val baseCurrency: String
    )
}
