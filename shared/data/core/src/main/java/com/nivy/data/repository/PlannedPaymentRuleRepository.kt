package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.PlannedPaymentRuleDao
import com.nivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.nivy.data.model.AccountId
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.repository.mapper.PlannedPaymentRuleMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Rules that aren't valid are left out. */
class PlannedPaymentRuleRepository @Inject constructor(
    private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
    private val writePlannedPaymentRuleDao: WritePlannedPaymentRuleDao,
    private val mapper: PlannedPaymentRuleMapper,
    private val dispatchersProvider: DispatchersProvider,
) {

    suspend fun findAllByOneTime(oneTime: Boolean): List<PlannedPaymentRule> =
        withContext(dispatchersProvider.io) {
            plannedPaymentRuleDao.findAllByOneTime(oneTime)
                .mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
        }

    suspend fun findById(id: PlannedPaymentRuleId): PlannedPaymentRule? =
        withContext(dispatchersProvider.io) {
            plannedPaymentRuleDao.findById(id.value)
                ?.let { with(mapper) { it.toDomain() }.getOrNull() }
        }

    suspend fun count(): Long = withContext(dispatchersProvider.io) {
        plannedPaymentRuleDao.countPlannedPayments()
    }

    suspend fun save(value: PlannedPaymentRule): Unit = withContext(dispatchersProvider.io) {
        writePlannedPaymentRuleDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun deleteById(id: PlannedPaymentRuleId): Unit = withContext(dispatchersProvider.io) {
        writePlannedPaymentRuleDao.deleteById(id.value)
    }

    suspend fun deleteAllByAccountId(accountId: AccountId): Unit =
        withContext(dispatchersProvider.io) {
            writePlannedPaymentRuleDao.deleteAllByAccountId(accountId.value)
        }

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writePlannedPaymentRuleDao.deleteAll()
    }
}
