package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.AccountValueEntity
import java.util.UUID

@Dao
interface AccountValueDao {
    @Query("SELECT * FROM account_values ORDER BY time")
    suspend fun findAll(): List<AccountValueEntity>

    @Query("SELECT * FROM account_values WHERE accountId = :accountId ORDER BY time")
    suspend fun findByAccountId(accountId: UUID): List<AccountValueEntity>
}
