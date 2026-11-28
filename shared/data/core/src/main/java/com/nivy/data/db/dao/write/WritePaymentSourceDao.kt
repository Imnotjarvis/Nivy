package com.nivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nivy.data.db.entity.PaymentSourceEntity
import java.util.UUID

@Dao
interface WritePaymentSourceDao {
    @Upsert
    suspend fun save(value: PaymentSourceEntity)

    @Upsert
    suspend fun saveMany(values: List<PaymentSourceEntity>)

    @Query("DELETE FROM payment_sources WHERE kind = :kind AND id = :id")
    suspend fun delete(kind: String, id: String)

    @Query("DELETE FROM payment_sources WHERE accountId = :accountId")
    suspend fun deleteAllByAccountId(accountId: UUID)

    @Query("DELETE FROM payment_sources")
    suspend fun deleteAll()
}
