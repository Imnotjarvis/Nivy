package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.BudgetDao
import com.nivy.data.db.dao.write.WriteBudgetDao
import com.nivy.data.model.Budget
import com.nivy.data.model.BudgetId
import com.nivy.data.repository.mapper.BudgetMapper
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
        budgetDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.orLogged() }
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

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeBudgetDao.deleteAll()
    }
}
