package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.CreditCardDao
import com.ivy.data.db.dao.write.WriteCreditCardDao
import com.ivy.data.model.AccountId
import com.ivy.data.model.CreditCard
import com.ivy.data.model.CreditCardId
import com.ivy.data.repository.mapper.CreditCardMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Cards that aren't valid are left out. */
class CreditCardRepository @Inject constructor(
    private val creditCardDao: CreditCardDao,
    private val writeCreditCardDao: WriteCreditCardDao,
    private val mapper: CreditCardMapper,
    private val dispatchersProvider: DispatchersProvider,
) {
    suspend fun findAll(): List<CreditCard> = withContext(dispatchersProvider.io) {
        creditCardDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
    }

    suspend fun findById(id: CreditCardId): CreditCard? = withContext(dispatchersProvider.io) {
        creditCardDao.findById(id.value)?.let { with(mapper) { it.toDomain() }.getOrNull() }
    }

    suspend fun findByAccount(accountId: AccountId): List<CreditCard> =
        withContext(dispatchersProvider.io) {
            creditCardDao.findByAccountId(accountId.value)
                .mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
        }

    /** Saves the [cards] of [accountId], and deletes the ones it had that aren't among them. */
    suspend fun saveAccountCards(accountId: AccountId, cards: List<CreditCard>): Unit =
        withContext(dispatchersProvider.io) {
            val kept = cards.map { it.id }.toSet()
            creditCardDao.findByAccountId(accountId.value)
                .filter { CreditCardId(it.id) !in kept }
                .forEach { writeCreditCardDao.deleteById(it.id) }
            writeCreditCardDao.saveMany(cards.map { with(mapper) { it.toEntity() } })
        }

    suspend fun deleteAllByAccountId(accountId: AccountId): Unit =
        withContext(dispatchersProvider.io) {
            writeCreditCardDao.deleteAllByAccountId(accountId.value)
        }

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeCreditCardDao.deleteAll()
    }
}
