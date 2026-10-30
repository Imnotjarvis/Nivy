package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.data.model.LoanRecord
import com.ivy.data.model.LoanRecordId
import com.ivy.data.repository.mapper.LoanMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Loans and their records. Items that aren't valid are left out. */
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val writeLoanDao: WriteLoanDao,
    private val loanRecordDao: LoanRecordDao,
    private val writeLoanRecordDao: WriteLoanRecordDao,
    private val mapper: LoanMapper,
    private val dispatchersProvider: DispatchersProvider,
) {
    /** Sorted by [Loan.orderNum]. */
    suspend fun findAll(): List<Loan> = withContext(dispatchersProvider.io) {
        loanDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
    }

    suspend fun findById(id: LoanId): Loan? = withContext(dispatchersProvider.io) {
        loanDao.findById(id.value)?.let { with(mapper) { it.toDomain() }.getOrNull() }
    }

    suspend fun findMaxOrderNum(): Double = withContext(dispatchersProvider.io) {
        loanDao.findMaxOrderNum() ?: 0.0
    }

    suspend fun save(value: Loan): Unit = withContext(dispatchersProvider.io) {
        writeLoanDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun saveMany(values: List<Loan>): Unit = withContext(dispatchersProvider.io) {
        writeLoanDao.saveMany(values.map { with(mapper) { it.toEntity() } })
    }

    suspend fun deleteById(id: LoanId): Unit = withContext(dispatchersProvider.io) {
        writeLoanDao.deleteById(id.value)
    }

    /** Newest first. */
    suspend fun findAllRecords(): List<LoanRecord> = withContext(dispatchersProvider.io) {
        loanRecordDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
    }

    /** Newest first. */
    suspend fun findRecordsByLoanId(loanId: LoanId): List<LoanRecord> =
        withContext(dispatchersProvider.io) {
            loanRecordDao.findAllByLoanId(loanId.value)
                .mapNotNull { with(mapper) { it.toDomain() }.getOrNull() }
        }

    suspend fun findRecordById(id: LoanRecordId): LoanRecord? =
        withContext(dispatchersProvider.io) {
            loanRecordDao.findById(id.value)?.let { with(mapper) { it.toDomain() }.getOrNull() }
        }

    suspend fun saveRecord(value: LoanRecord): Unit = withContext(dispatchersProvider.io) {
        writeLoanRecordDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun deleteRecordById(id: LoanRecordId): Unit = withContext(dispatchersProvider.io) {
        writeLoanRecordDao.deleteById(id.value)
    }
}
