package com.nivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nivy.data.db.entity.QuickAddEntity
import java.util.UUID

@Dao
interface WriteQuickAddDao {
    @Upsert
    suspend fun save(value: QuickAddEntity)

    @Upsert
    suspend fun saveMany(values: List<QuickAddEntity>)

    // An account's: its incomes and expenses, and the transfers out of it or into it.
    @Query("DELETE FROM quick_adds WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteAllByAccountId(accountId: UUID)

    @Query("DELETE FROM quick_adds WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM quick_adds")
    suspend fun deleteAll()
}
