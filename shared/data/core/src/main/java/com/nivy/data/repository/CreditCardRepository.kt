package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.CreditCardDao
import com.nivy.data.db.dao.write.WriteCreditCardDao
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.repository.mapper.CreditCardMapper
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

    /**
     * Saves the [cards] of [accountId], and deletes the ones it had that aren't among them, in one
     * database transaction: a save cut short leaves the cards, and what was bought with them, as
     * they were.
     */
    suspend fun saveAccountCards(accountId: AccountId, cards: List<CreditCard>): Unit =
        withContext(dispatchersProvider.io) {
            val kept = cards.map { it.id }.toSet()
            val removed = creditCardDao.findByAccountId(accountId.value)
                .filter { CreditCardId(it.id) !in kept }
                .map { it.id }
            writeCreditCardDao.replaceCards(removed, cards.map { with(mapper) { it.toEntity() } })
        }

    suspend fun deleteAllByAccountId(accountId: AccountId): Unit =
        withContext(dispatchersProvider.io) {
            writeCreditCardDao.deleteAllByAccountId(accountId.value)
        }

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeCreditCardDao.deleteAll()
    }
}
