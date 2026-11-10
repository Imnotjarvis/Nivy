package com.nivy.data.repository

import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.dao.write.WriteTransactionDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.repository.mapper.TransactionMapper
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val mapper: TransactionMapper,
    private val transactionDao: TransactionDao,
    private val writeTransactionDao: WriteTransactionDao,
    private val dispatchersProvider: DispatchersProvider,
    private val tagRepository: TagRepository,
    private val databaseTransaction: DatabaseTransaction,
) {
    suspend fun findAll(): List<Transaction> = retrieveTrns(dbCall = transactionDao::findAll)

    suspend fun findAllBetween(
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllBetween(startDate, endDate)
        }
    )

    suspend fun findAllByAccountAndBetween(
        accountId: AccountId,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllByAccountAndBetween(
                accountId = accountId.value,
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllToAccountAndBetween(
        toAccountId: AccountId,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllToAccountAndBetween(
                toAccountId = toAccountId.value,
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    /**
     * What [accountId]'s transactions dated from [startDate] to [endDate] add up to, as if read with
     * [findAllByAccountAndBetween] and [findAllToAccountAndBetween] and added up, but summed by the
     * database: an account's history can run to thousands of transactions.
     */
    suspend fun balance(
        accountId: AccountId,
        startDate: Instant,
        endDate: Instant
    ): Double = withContext(dispatchersProvider.io) {
        transactionDao.balance(accountId = accountId.value, startDate = startDate, endDate = endDate)
    }

    suspend fun findAllDueToBetween(
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllDueToBetween(
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllDueToBetweenByCategory(
        startDate: Instant,
        endDate: Instant,
        categoryId: CategoryId
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllDueToBetweenByCategory(
                startDate = startDate,
                endDate = endDate,
                categoryId = categoryId.value
            )
        }
    )

    suspend fun findAllDueToBetweenByCategoryUnspecified(
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllDueToBetweenByCategoryUnspecified(
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllDueToBetweenByAccount(
        startDate: Instant,
        endDate: Instant,
        accountId: AccountId
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllDueToBetweenByAccount(
                startDate = startDate,
                endDate = endDate,
                accountId = accountId.value
            )
        }
    )

    suspend fun findAllByCategoryAndTypeAndBetween(
        categoryId: UUID,
        type: TransactionType,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllByCategoryAndTypeAndBetween(
                categoryId = categoryId,
                type = type,
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllUnspecifiedAndTypeAndBetween(
        type: TransactionType,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllUnspecifiedAndTypeAndBetween(
                type = type,
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllUnspecifiedAndBetween(
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllUnspecifiedAndBetween(
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllByCategoryAndBetween(
        categoryId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllByCategoryAndBetween(
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate
            )
        }
    )

    suspend fun findAllByRecurringRuleId(recurringRuleId: UUID): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllByRecurringRuleId(recurringRuleId)
        }
    )

    suspend fun findById(
        id: TransactionId
    ): Transaction? = withContext(dispatchersProvider.io) {
        transactionDao.findById(id.value)?.let {
            with(mapper) { it.toDomain() }.orLogged()
        }
    }

    suspend fun findByIds(ids: List<TransactionId>): List<Transaction> = retrieveTrns(
        dbCall = {
            // SQLite before 3.32 (Android 9 to 11) allows at most 999 variables a query.
            ids.map { it.value }.chunked(MAX_SQL_LITE_QUERY_SIZE)
                .flatMap { transactionDao.findByIds(it) }
        }
    )

    suspend fun save(value: Transaction) {
        withContext(dispatchersProvider.io) {
            writeTransactionDao.save(
                with(mapper) { value.toEntity() }
            )
        }
    }

    suspend fun saveMany(value: List<Transaction>) {
        withContext(dispatchersProvider.io) {
            writeTransactionDao.saveMany(
                value.map { with(mapper) { it.toEntity() } }
            )
        }
    }

    suspend fun deleteById(id: TransactionId) {
        deleting { writeTransactionDao.deleteById(id.value) }
    }

    suspend fun deleteAllByAccountId(accountId: AccountId) {
        deleting { writeTransactionDao.deleteAllByAccountId(accountId.value) }
    }

    suspend fun deletedByRecurringRuleIdAndNoDateTime(recurringRuleId: UUID) {
        deleting { writeTransactionDao.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId) }
    }

    /**
     * Runs [delete], then deletes the tag links of what it deleted, in one database transaction: a
     * deleted transaction takes its links with it.
     */
    private suspend fun deleting(delete: suspend () -> Unit) {
        withContext(dispatchersProvider.io) {
            databaseTransaction.run {
                delete()
                tagRepository.deleteLinksOfDeletedTransactions()
            }
        }
    }

    suspend fun deleteAll() {
        withContext(dispatchersProvider.io) {
            writeTransactionDao.deleteAll()
        }
    }

    /** When the first paid transaction was made, or null without any. */
    suspend fun findFirstPaidTime(): Instant? = withContext(dispatchersProvider.io) {
        transactionDao.findFirstDateTime()
    }

    suspend fun countHappenedTransactions(): Long = withContext(dispatchersProvider.io) {
        transactionDao.countHappenedTransactions()
    }

    suspend fun findLoanTransaction(loanId: UUID): Transaction? =
        withContext(dispatchersProvider.io) {
            transactionDao.findLoanTransaction(loanId)?.let {
                with(mapper) { it.toDomain() }.orLogged()
            }
        }

    suspend fun findLoanRecordTransaction(loanRecordId: UUID): Transaction? =
        withContext(dispatchersProvider.io) {
            transactionDao.findLoanRecordTransaction(loanRecordId)?.let {
                with(mapper) { it.toDomain() }.orLogged()
            }
        }

    suspend fun findAllByLoanId(loanId: UUID): List<Transaction> = retrieveTrns(
        dbCall = {
            transactionDao.findAllByLoanId(loanId)
        }
    )

    private suspend fun retrieveTrns(
        dbCall: suspend () -> List<TransactionEntity>,
    ): List<Transaction> = withContext(dispatchersProvider.io) {
        dbCall().mapNotNull {
            with(mapper) { it.toDomain() }.orLogged()
        }
    }
}

private const val MAX_SQL_LITE_QUERY_SIZE = 999
