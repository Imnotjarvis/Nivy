package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import java.time.Instant
import java.util.UUID

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC, dueDate ASC")
    suspend fun findAll(): List<TransactionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions)")
    suspend fun hasTransactions(): Boolean

    @Query(
        "SELECT * FROM transactions WHERE type = :type and accountId = :accountId ORDER BY dateTime DESC"
    )
    suspend fun findAllByTypeAndAccount(
        type: TransactionType,
        accountId: UUID
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE type = :type and toAccountId = :toAccountId ORDER BY dateTime DESC"
    )
    suspend fun findAllTransfersToAccount(
        toAccountId: UUID,
        type: TransactionType = TransactionType.TRANSFER
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE dateTime >= :startDate AND dateTime <= :endDate ORDER BY dateTime DESC"
    )
    suspend fun findAllBetween(
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE accountId = :accountId AND dateTime >= :startDate AND dateTime <= :endDate " +
            "ORDER BY dateTime DESC"
    )
    suspend fun findAllByAccountAndBetween(
        accountId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE (categoryId = :categoryId) AND dateTime >= :startDate AND dateTime <= :endDate " +
            "ORDER BY dateTime DESC"
    )
    suspend fun findAllByCategoryAndBetween(
        categoryId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE (categoryId IS NULL) AND dateTime >= :startDate AND dateTime <= :endDate " +
            "ORDER BY dateTime DESC"
    )
    suspend fun findAllUnspecifiedAndBetween(
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE (categoryId = :categoryId) AND type = :type AND dateTime >= :startDate AND dateTime <= :endDate " +
            "ORDER BY dateTime DESC"
    )
    suspend fun findAllByCategoryAndTypeAndBetween(
        categoryId: UUID,
        type: TransactionType,
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE (categoryId IS NULL) AND type = :type AND dateTime >= :startDate AND dateTime <= :endDate " +
            "ORDER BY dateTime DESC"
    )
    suspend fun findAllUnspecifiedAndTypeAndBetween(
        type: TransactionType,
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE toAccountId = :toAccountId AND dateTime >= :startDate AND dateTime <= :endDate " +
            "ORDER BY dateTime DESC"
    )
    suspend fun findAllToAccountAndBetween(
        toAccountId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC"
    )
    suspend fun findAllDueToBetween(
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE dueDate >= :startDate AND dueDate <= :endDate AND (categoryId = :categoryId) " +
            "ORDER BY dateTime DESC, dueDate ASC"
    )
    suspend fun findAllDueToBetweenByCategory(
        startDate: Instant,
        endDate: Instant,
        categoryId: UUID
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE dueDate >= :startDate AND dueDate <= :endDate AND (categoryId IS NULL) " +
            "ORDER BY dateTime DESC, dueDate ASC"
    )
    suspend fun findAllDueToBetweenByCategoryUnspecified(
        startDate: Instant,
        endDate: Instant,
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE dueDate >= :startDate AND dueDate <= :endDate AND accountId = :accountId " +
            "ORDER BY dateTime DESC, dueDate ASC"
    )
    suspend fun findAllDueToBetweenByAccount(
        startDate: Instant,
        endDate: Instant,
        accountId: UUID
    ): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE recurringRuleId = :recurringRuleId ORDER BY dateTime DESC"
    )
    suspend fun findAllByRecurringRuleId(recurringRuleId: UUID): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: UUID): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id in (:ids)")
    suspend fun findByIds(ids: List<UUID>): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE dateTime IS NOT null")
    suspend fun countHappenedTransactions(): Long

    // Smart Title Suggestions
    @Query("SELECT * FROM transactions WHERE title LIKE :pattern")
    suspend fun findAllByTitleMatchingPattern(pattern: String): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE title LIKE :pattern")
    suspend fun countByTitleMatchingPattern(
        pattern: String,
    ): Long

    @Query("SELECT * FROM transactions WHERE (categoryId = :categoryId) ORDER BY dateTime DESC")
    suspend fun findAllByCategory(
        categoryId: UUID,
    ): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE title LIKE :pattern AND categoryId = :categoryId")
    suspend fun countByTitleMatchingPatternAndCategoryId(
        pattern: String,
        categoryId: UUID
    ): Long

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateTime DESC")
    suspend fun findAllByAccount(
        accountId: UUID
    ): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE title LIKE :pattern AND accountId = :accountId")
    suspend fun countByTitleMatchingPatternAndAccountId(
        pattern: String,
        accountId: UUID
    ): Long

    @Query("SELECT * FROM transactions WHERE loanId = :loanId AND loanRecordId IS NULL")
    suspend fun findLoanTransaction(
        loanId: UUID
    ): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE loanRecordId = :loanRecordId")
    suspend fun findLoanRecordTransaction(
        loanRecordId: UUID
    ): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE loanId = :loanId")
    suspend fun findAllByLoanId(
        loanId: UUID
    ): List<TransactionEntity>
}
