package com.nivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nivy.data.db.entity.AccountValueEntity
import java.util.UUID

@Dao
interface WriteAccountValueDao {
    @Upsert
    suspend fun save(value: AccountValueEntity)

    @Upsert
    suspend fun saveMany(values: List<AccountValueEntity>)

    @Query("DELETE FROM account_values WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM account_values WHERE accountId = :accountId")
    suspend fun deleteAllByAccountId(accountId: UUID)

    @Query("DELETE FROM account_values")
    suspend fun deleteAll()
}
