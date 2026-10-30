package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.BudgetDao
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.data.model.Budget
import com.ivy.data.model.BudgetId
import com.ivy.data.repository.mapper.BudgetMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val writeBudgetDao: WriteBudgetDao,
    private val mapper: BudgetMapper,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** Sorted by [Budget.orderNum]. Budgets that aren't valid are left out. */
    suspend fun findAll(): List<Budget> = withContext(dispatchersProvider.io) {
        budgetDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
    }

    suspend fun findMaxOrderNum(): Double = withContext(dispatchersProvider.io) {
        budgetDao.findMaxOrderNum() ?: 0.0
    }

    suspend fun save(value: Budget): Unit = withContext(dispatchersProvider.io) {
        writeBudgetDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun saveMany(values: List<Budget>): Unit = withContext(dispatchersProvider.io) {
        writeBudgetDao.saveMany(values.map { with(mapper) { it.toEntity() } })
    }

    suspend fun deleteById(id: BudgetId): Unit = withContext(dispatchersProvider.io) {
        writeBudgetDao.deleteById(id.value)
    }
}
