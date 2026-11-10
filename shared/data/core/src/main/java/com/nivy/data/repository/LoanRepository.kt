package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.LoanDao
import com.nivy.data.db.dao.read.LoanRecordDao
import com.nivy.data.db.dao.write.WriteLoanDao
import com.nivy.data.db.dao.write.WriteLoanRecordDao
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.repository.mapper.LoanMapper
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
        loanDao.findAll().mapNotNull { with(mapper) { it.toDomain() }.orLogged() }
    }

    suspend fun findById(id: LoanId): Loan? = withContext(dispatchersProvider.io) {
        loanDao.findById(id.value)?.let { with(mapper) { it.toDomain() }.orLogged() }
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
    suspend fun findRecordsByLoanId(loanId: LoanId): List<LoanRecord> =
        withContext(dispatchersProvider.io) {
            loanRecordDao.findAllByLoanId(loanId.value)
                .mapNotNull { with(mapper) { it.toDomain() }.orLogged() }
        }

    suspend fun findRecordById(id: LoanRecordId): LoanRecord? =
        withContext(dispatchersProvider.io) {
            loanRecordDao.findById(id.value)?.let { with(mapper) { it.toDomain() }.orLogged() }
        }

    suspend fun saveRecord(value: LoanRecord): Unit = withContext(dispatchersProvider.io) {
        writeLoanRecordDao.save(with(mapper) { value.toEntity() })
    }

    suspend fun deleteRecordById(id: LoanRecordId): Unit = withContext(dispatchersProvider.io) {
        writeLoanRecordDao.deleteById(id.value)
    }

    /** Deletes every loan and every record. */
    suspend fun deleteAll(): Unit = withContext(dispatchersProvider.io) {
        writeLoanRecordDao.deleteAll()
        writeLoanDao.deleteAll()
    }
}
