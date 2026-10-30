package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.model.AccountId
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.repository.mapper.PlannedPaymentRuleMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Rules that aren't valid are left out. */
class PlannedPaymentRuleRepository @Inject constructor(
    private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
    private val writePlannedPaymentRuleDao: WritePlannedPaymentRuleDao,
    private val mapper: PlannedPaymentRuleMapper,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** Largest amount first, then by start date. */
    suspend fun findAll(): List<PlannedPaymentRule> = withContext(dispatchersProvider.io) {
        plannedPaymentRuleDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
    }

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
}
