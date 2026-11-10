package com.nivy.domain.usecase.planned

import com.nivy.base.model.TransactionType
import com.nivy.base.model.incrementDate
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PlannedPaymentInterval
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.TransactionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Creates the planned (unpaid) transactions of a planned payment rule. */
class PlannedPaymentsGenerator @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val timeProvider: TimeProvider,
    private val databaseTransaction: DatabaseTransaction,
) {
    /**
     * Replaces the rule's unpaid transactions: one for a one-time rule, and for a recurring one,
     * up to [GeneratedInstancesLimit] within three years. The occurrences already handled stay as
     * they are: the paid ones, and the skipped ones (a skip deletes its transaction).
     *
     * On the dates of [previous], the rule as it was, an occurrence gone from before the latest
     * one was skipped. On new dates, or for a new rule, those up to the last paid one count as
     * handled, until the first unpaid one.
     */
    suspend fun generate(rule: PlannedPaymentRule, previous: PlannedPaymentRule?) {
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id.value)
        val asset = accountRepository.findById(rule.account)?.asset
        val unpaid = asset?.let { unpaidOccurrences(rule, previous, existing, it) }.orEmpty()
        // One database transaction: cut short, the rule keeps the payments it had.
        databaseTransaction.run {
            transactionRepository.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId = rule.id.value)
            transactionRepository.saveMany(unpaid)
        }
    }

    // The unpaid occurrences [generate] plans for [rule], given its [existing] transactions.
    private fun unpaidOccurrences(
        rule: PlannedPaymentRule,
        previous: PlannedPaymentRule?,
        existing: List<Transaction>,
        asset: AssetCode,
    ): List<Transaction> {
        val paidDates = existing.filter { it.settled }.mapTo(HashSet()) { it.dueDate() }
        val interval = rule.interval
        if (interval == null) {
            return if (paidDates.isEmpty()) listOf(plannedTransaction(rule, asset, rule.startDate)) else emptyList()
        }

        val lastPaid = paidDates.maxOrNull()
        val unpaidDates = existing.filterNot { it.settled }.mapTo(HashSet()) { it.time }
        val firstUnpaid = unpaidDates.minOrNull()
        val latest = existing.maxOfOrNull { it.dueDate() }
        val sameDates = previous != null && previous.startDate == rule.startDate && previous.interval == interval
        return occurrences(rule, interval, asset, limit = GeneratedInstancesLimit) { date ->
            date !in paidDates && if (sameDates) {
                date in unpaidDates || latest == null || date > latest
            } else {
                val afterHandled = lastPaid == null || date > lastPaid
                val stillUnpaid = firstUnpaid != null && date >= firstUnpaid
                afterHandled || stillUnpaid
            }
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
        // One insert: a top-up cut short adds none.
        transactionRepository.saveMany(
            occurrences(rule, interval, asset, limit = GeneratedInstancesLimit - unpaid) { it > latest }
        )
    }

    /**
     * The rule's occurrences that [include] accepts, up to [limit] of them and three years from
     * now (or from its start, if that is later). Each date counts from the start date, so a
     * monthly rule from 31 January falls on 28 February and then 31 March.
     */
    private fun occurrences(
        rule: PlannedPaymentRule,
        interval: PlannedPaymentInterval,
        asset: AssetCode,
        limit: Int,
        include: (Instant) -> Boolean,
    ): List<Transaction> {
        val end = maxOf(rule.startDate, timeProvider.utcNow()).plusSeconds(GenerationPeriodSeconds)
        val planned = mutableListOf<Transaction>()
        var step = 0L
        while (planned.size < limit) {
            val date = interval.type.incrementDate(
                rule.startDate,
                intervalN = step++ * interval.n.value,
                zone = timeProvider.getZoneId(),
            )
            if (!date.isBefore(end)) break
            if (include(date)) planned += plannedTransaction(rule, asset, dueDate = date)
        }
        return planned
    }

    /** The date a transaction was planned for: a paid one keeps it in its metadata. */
    private fun Transaction.dueDate(): Instant = metadata.paidForDateTime ?: time

    /** The unpaid transaction [rule] plans for [dueDate]. */
    private fun plannedTransaction(rule: PlannedPaymentRule, asset: AssetCode, dueDate: Instant): Transaction {
        val id = TransactionId(UUID.randomUUID())
        val value = PositiveValue(amount = rule.amount, asset = asset)
        val metadata = TransactionMetadata(
            recurringRuleId = rule.id.value,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
            // Paid with the rule's card, it counts when the card charges it.
            creditCard = rule.creditCard,
        )
        return if (rule.type == TransactionType.INCOME) {
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
    }

    private companion object {
        const val GeneratedInstancesLimit = 72
        const val GenerationPeriodSeconds = 94_608_000L // three years
    }
}
