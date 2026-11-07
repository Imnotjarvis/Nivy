package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.AccountValueDao
import com.nivy.data.db.dao.write.WriteAccountValueDao
import com.nivy.data.model.AccountId
import com.nivy.data.model.AccountValue
import com.nivy.data.model.AccountValueId
import com.nivy.data.repository.mapper.AccountValueMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Values that aren't valid are left out. */
class AccountValueRepository @Inject constructor(
    private val accountValueDao: AccountValueDao,
    private val writeAccountValueDao: WriteAccountValueDao,
    private val mapper: AccountValueMapper,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** The values entered for [accountId], oldest first. */
    suspend fun findByAccount(accountId: AccountId): List<AccountValue> =
        withContext(dispatchersProvider.io) {
            accountValueDao.findByAccountId(accountId.value)
                .mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
        }

    suspend fun save(value: AccountValue): Unit = withContext(dispatchersProvider.io) {
        writeAccountValueDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun deleteById(id: AccountValueId): Unit = withContext(dispatchersProvider.io) {
        writeAccountValueDao.deleteById(id.value)
    }

    suspend fun deleteAllByAccountId(accountId: AccountId): Unit =
        withContext(dispatchersProvider.io) {
            writeAccountValueDao.deleteAllByAccountId(accountId.value)
        }

    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeAccountValueDao.deleteAll()
    }
}
