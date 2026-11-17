package com.nivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.nivy.data.db.entity.QuickAddEntity
import java.util.UUID

@Dao
interface QuickAddDao {
    @Query("SELECT * FROM quick_adds ORDER BY orderNum ASC")
    suspend fun findAll(): List<QuickAddEntity>

    @Query("SELECT * FROM quick_adds WHERE id = :id")
    suspend fun findById(id: UUID): QuickAddEntity?
}
