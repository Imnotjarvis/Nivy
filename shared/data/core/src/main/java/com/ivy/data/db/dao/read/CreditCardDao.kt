package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.CreditCardEntity
import java.util.UUID

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY name COLLATE NOCASE")
    suspend fun findAll(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun findById(id: UUID): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE accountId = :accountId ORDER BY name COLLATE NOCASE")
    suspend fun findByAccountId(accountId: UUID): List<CreditCardEntity>
}
