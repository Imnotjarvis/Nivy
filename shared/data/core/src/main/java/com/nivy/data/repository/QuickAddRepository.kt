package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.QuickAddDao
import com.nivy.data.db.dao.write.WriteQuickAddDao
import com.nivy.data.model.AccountId
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.QuickAddId
import com.nivy.data.repository.mapper.QuickAddMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Quick adds that aren't valid are left out. */
class QuickAddRepository @Inject constructor(
    private val quickAddDao: QuickAddDao,
    private val writeQuickAddDao: WriteQuickAddDao,
    private val mapper: QuickAddMapper,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** In their order. */
    suspend fun findAll(): List<QuickAdd> = withContext(dispatchersProvider.io) {
        quickAddDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.orLogged() }
    }

    suspend fun findById(id: QuickAddId): QuickAdd? = withContext(dispatchersProvider.io) {
        quickAddDao.findById(id.value)?.let { with(mapper) { it.toDomain() }.orLogged() }
    }

    suspend fun save(value: QuickAdd): Unit = withContext(dispatchersProvider.io) {
        writeQuickAddDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun saveMany(values: List<QuickAdd>): Unit = withContext(dispatchersProvider.io) {
        writeQuickAddDao.saveMany(values.map { with(mapper) { it.toEntity() } })
    }

    suspend fun deleteById(id: QuickAddId): Unit = withContext(dispatchersProvider.io) {
        writeQuickAddDao.deleteById(id.value)
    }

    suspend fun deleteAllByAccountId(accountId: AccountId): Unit = withContext(dispatchersProvider.io) {
        writeQuickAddDao.deleteAllByAccountId(accountId.value)
    }

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeQuickAddDao.deleteAll()
    }
}
