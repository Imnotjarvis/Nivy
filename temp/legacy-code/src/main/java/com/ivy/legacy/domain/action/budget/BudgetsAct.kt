package com.ivy.wallet.domain.action.budget

import com.ivy.data.db.dao.read.BudgetDao
import com.ivy.legacy.datamodel.Budget
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetsAct @Inject constructor(
    private val budgetDao: BudgetDao
) {
    suspend operator fun invoke(): ImmutableList<Budget> = withContext(Dispatchers.IO) {
        budgetDao.findAll().map { it.toLegacyDomain() }.toImmutableList()
    }
}
