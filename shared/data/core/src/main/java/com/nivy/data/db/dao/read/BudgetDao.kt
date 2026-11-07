package com.nivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.nivy.data.db.entity.BudgetEntity
import java.util.*

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY orderId ASC")
    suspend fun findAll(): List<BudgetEntity>

    @Query("SELECT MAX(orderId) FROM budgets")
    suspend fun findMaxOrderNum(): Double?
}
