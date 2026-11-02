package com.ivy.domain.usecase.planned

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PlannedPaymentInterval
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.base.model.incrementDate
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Creates the planned (unpaid) transactions of a planned payment rule. */
class PlannedPaymentsGenerator @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val timeProvider: TimeProvider,
) {
    /**
     * Replaces the rule's unpaid transactions: one for a one-time rule, and for a recurring one,
     * up to [GeneratedInstancesLimit] within three years. Occurrences already handled stay as they
     * are: the paid ones, and those before the first unpaid one up to the last paid one, which
     * were paid or skipped (a skip deletes its transaction).
     */
    suspend fun generate(rule: PlannedPaymentRule) {
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id.value)
        transactionRepository.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId = rule.id.value)
        val asset = accountRepository.findById(rule.account)?.asset ?: return
        val paidDates = existing.filter { it.settled }.mapTo(HashSet()) { it.dueDate() }

        val interval = rule.interval
        if (interval == null) {
            if (paidDates.isEmpty()) save(rule, asset, dueDate = rule.startDate)
            return
        }

        val lastPaid = paidDates.maxOrNull()
        val firstUnpaid = existing.filterNot { it.settled }.minOfOrNull { it.time }
        saveOccurrences(rule, interval, asset, limit = GeneratedInstancesLimit) { date ->
            val afterHandled = lastPaid == null || date > lastPaid
            val stillUnpaid = firstUnpaid != null && date >= firstUnpaid
            date !in paidDates && (afterHandled || stillUnpaid)
        }
    }

    /**
     * Adds occurrences after the rule's latest one, so that up to [GeneratedInstancesLimit] stay
     * unpaid. Nothing is deleted or re-created, so it is safe after paying or skipping.
     */
    suspend fun topUp(rule: PlannedPaymentRule) {
        val interval = rule.interval ?: return
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id.value)
        val latest = existing.maxOfOrNull { it.dueDate() } ?: return
        val unpaid = existing.count { !it.settled }
        val asset = accountRepository.findById(rule.account)?.asset ?: return
        saveOccurrences(rule, interval, asset, limit = GeneratedInstancesLimit - unpaid) { it > latest }
    }

    /**
     * Saves the rule's occurrences that [include] accepts, up to [limit] of them and three years
     * from now (or from its start, if that is later). Each date counts from the start date, so a
     * monthly rule from 31 January falls on 28 February and then 31 March.
     */
    private suspend fun saveOccurrences(
        rule: PlannedPaymentRule,
        interval: PlannedPaymentInterval,
        asset: AssetCode,
        limit: Int,
        include: (Instant) -> Boolean,
    ) {
        val end = maxOf(rule.startDate, timeProvider.utcNow()).plusSeconds(GenerationPeriodSeconds)
        var saved = 0
        var occurrence = 0L
        while (saved < limit) {
            val date = interval.type.incrementDate(
                rule.startDate,
                intervalN = occurrence++ * interval.n.value,
                zone = timeProvider.getZoneId(),
            )
            if (!date.isBefore(end)) break
            if (include(date)) {
                save(rule, asset, dueDate = date)
                saved++
            }
        }
    }

    /** The date a transaction was planned for: a paid one keeps it in its metadata. */
    private fun Transaction.dueDate(): Instant = metadata.paidForDateTime ?: time

    private suspend fun save(rule: PlannedPaymentRule, asset: AssetCode, dueDate: Instant) {
        val id = TransactionId(UUID.randomUUID())
        val value = PositiveValue(amount = rule.amount, asset = asset)
        val metadata = TransactionMetadata(
            recurringRuleId = rule.id.value,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
        )
        transactionRepository.save(
            if (rule.type == TransactionType.INCOME) {
                Income(
                    id = id,
                    title = rule.title,
                    description = rule.description,
                    category = rule.category,
                    time = dueDate,
                    settled = false,
                    metadata = metadata,
                    tags = emptyList(),
                    value = value,
                    account = rule.account,
                )
            } else {
                Expense(
                    id = id,
                    title = rule.title,
                    description = rule.description,
                    category = rule.category,
                    time = dueDate,
                    settled = false,
                    metadata = metadata,
                    tags = emptyList(),
                    value = value,
                    account = rule.account,
                )
            }
        )
    }

    private companion object {
        const val GeneratedInstancesLimit = 72
        const val GenerationPeriodSeconds = 94_608_000L // three years
    }
}
