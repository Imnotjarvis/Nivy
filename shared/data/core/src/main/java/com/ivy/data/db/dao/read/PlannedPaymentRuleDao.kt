package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import java.util.*

@Dao
interface PlannedPaymentRuleDao {
    @Query("SELECT * FROM planned_payment_rules ORDER BY amount DESC, startDate ASC")
    suspend fun findAll(): List<PlannedPaymentRuleEntity>

    @Query(
        "SELECT * FROM planned_payment_rules WHERE oneTime = :oneTime ORDER BY amount DESC, startDate ASC"
    )
    suspend fun findAllByOneTime(oneTime: Boolean): List<PlannedPaymentRuleEntity>

    @Query("SELECT * FROM planned_payment_rules WHERE id = :id")
    suspend fun findById(id: UUID): PlannedPaymentRuleEntity?

    @Query("SELECT COUNT(*) FROM planned_payment_rules")
    suspend fun countPlannedPayments(): Long
}
