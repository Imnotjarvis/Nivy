package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.LoanRecordEntity
import java.util.*

@Dao
interface LoanRecordDao {
    @Query("SELECT * FROM loan_records ORDER BY dateTime DESC")
    suspend fun findAll(): List<LoanRecordEntity>

    @Query("SELECT * FROM loan_records WHERE id = :id")
    suspend fun findById(id: UUID): LoanRecordEntity?

    @Query("SELECT * FROM loan_records WHERE loanId = :loanId ORDER BY dateTime DESC")
    suspend fun findAllByLoanId(loanId: UUID): List<LoanRecordEntity>
}
