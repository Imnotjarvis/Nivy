package com.nivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.nivy.data.db.entity.PaymentSourceEntity

@Dao
interface PaymentSourceDao {
    @Query("SELECT * FROM payment_sources")
    suspend fun findAll(): List<PaymentSourceEntity>
}
