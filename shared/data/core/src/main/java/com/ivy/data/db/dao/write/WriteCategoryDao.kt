package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ivy.data.db.entity.CategoryEntity
import java.util.UUID

@Dao
interface WriteCategoryDao {
    @Upsert
    suspend fun save(value: CategoryEntity)

    @Upsert
    suspend fun saveMany(values: List<CategoryEntity>)

    /** Deletes the category. Its transactions and planned payments stay, without a category. */
    @Transaction
    suspend fun deleteById(id: UUID) {
        uncategorizeTransactions(id)
        uncategorizePlannedPaymentRules(id)
        deleteCategoryRow(id)
    }

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id")
    suspend fun uncategorizeTransactions(id: UUID)

    @Query("UPDATE planned_payment_rules SET categoryId = NULL WHERE categoryId = :id")
    suspend fun uncategorizePlannedPaymentRules(id: UUID)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryRow(id: UUID)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
