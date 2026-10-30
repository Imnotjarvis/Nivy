package com.ivy.wallet.domain.action.category

import com.ivy.data.model.Category
import com.ivy.data.model.Transaction
import com.ivy.data.temp.migration.getAccountId
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.transaction.CalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.transaction.LegacyCalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryIncomeWithAccountFiltersAct @Inject constructor(
    private val calcTrnsIncomeExpenseAct: CalcTrnsIncomeExpenseAct
) {
    suspend operator fun invoke(input: Input): IncomeExpenseTransferPair = withContext(Dispatchers.IO) {
        val accountFilterSet = input.accountFilterList.map { it.id }.toHashSet()
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

@Deprecated("Uses legacy Transaction")
class LegacyCategoryIncomeWithAccountFiltersAct @Inject constructor(
    private val calcTrnsIncomeExpenseAct: LegacyCalcTrnsIncomeExpenseAct
) {
    suspend operator fun invoke(input: Input): IncomeExpenseTransferPair = withContext(Dispatchers.IO) {
        val accountFilterSet = input.accountFilterList.map { it.id }.toHashSet()
        val transactions = input.transactions
            .filter { it.categoryId == input.category?.id?.value }
            .filter { accountFilterSet.isEmpty() || accountFilterSet.contains(it.accountId) }
        calcTrnsIncomeExpenseAct(
            LegacyCalcTrnsIncomeExpenseAct.Input(
                transactions = transactions,
                baseCurrency = input.baseCurrency,
                accounts = input.accountFilterList
            )
        )
    }

    data class Input(
        val transactions: List<com.ivy.base.legacy.Transaction>,
        val accountFilterList: List<Account>,
        val category: Category?,
        val baseCurrency: String
    )
}
