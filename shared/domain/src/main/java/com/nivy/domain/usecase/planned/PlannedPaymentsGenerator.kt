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
import com.nivy.data.model.Transfer
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Creates the planned (unpaid) transactions of a planned payment rule, and removes a rule that has
 * ended: one with an end date and nothing left to pay, as a one-time rule goes once it's paid.
 */
class PlannedPaymentsGenerator @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val databaseTransaction: DatabaseTransaction,
) {
    /**
     * Replaces the rule's unpaid transactions: one for a one-time rule, and for a recurring one,
     * up to [GeneratedInstancesLimit] within three years and up to its end date. The occurrences
     * already handled stay as they are: the paid ones, and the skipped ones (a skip deletes its
     * transaction).
     *
     * On the dates of [previous], the rule as it was, an occurrence gone from before the latest
     * one was skipped. On new dates, or for a new rule, those up to the last paid one count as
     * handled, until the first unpaid one.
     */
    suspend fun generate(rule: PlannedPaymentRule, previous: PlannedPaymentRule?) {
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id.value)
        val values = values(rule)
        val unpaid = values?.let { unpaidOccurrences(rule, previous, existing, it) }.orEmpty()
        // One database transaction: cut short, the rule keeps the payments it had.
        databaseTransaction.run {
            transactionRepository.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId = rule.id.value)
            transactionRepository.saveMany(unpaid)
            if (values != null && unpaid.isEmpty()) removeIfEnded(rule)
        }
    }

    // The unpaid occurrences [generate] plans for [rule], given its [existing] transactions.
    private fun unpaidOccurrences(
        rule: PlannedPaymentRule,
        previous: PlannedPaymentRule?,
        existing: List<Transaction>,
        values: Values,
    ): List<Transaction> {
        val paidDates = existing.filter { it.settled }.mapTo(HashSet()) { it.dueDate() }
        val interval = rule.interval
        if (interval == null) {
            return if (paidDates.isEmpty()) listOf(plannedTransaction(rule, values, rule.startDate)) else emptyList()
        }

        val lastPaid = paidDates.maxOrNull()
        val unpaidDates = existing.filterNot { it.settled }.mapTo(HashSet()) { it.time }
        val firstUnpaid = unpaidDates.minOrNull()
        val latest = existing.maxOfOrNull { it.dueDate() }
        val sameDates = previous != null && previous.startDate == rule.startDate && previous.interval == interval
        return occurrences(rule, interval, values, limit = GeneratedInstancesLimit) { date ->
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
     * unpaid, up to its end date. Nothing is deleted or re-created, so it is safe after paying or
     * skipping. A rule left with nothing to pay, all of it paid or skipped up to its end, has ended.
     */
    suspend fun topUp(rule: PlannedPaymentRule) {
        val interval = rule.interval ?: return
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id.value)
        val values = values(rule) ?: return
        val unpaid = existing.count { !it.settled }
        // None at all when every occurrence so far was skipped: there's nothing to follow.
        val latest = existing.maxOfOrNull { it.dueDate() }
        val added = latest?.let {
            occurrences(rule, interval, values, limit = GeneratedInstancesLimit - unpaid) { it > latest }
        }.orEmpty()
        // One database transaction: a top-up cut short adds none and removes nothing.
        databaseTransaction.run {
            transactionRepository.saveMany(added)
            if (unpaid + added.size == 0) removeIfEnded(rule)
        }
    }

    // A rule with an end date and nothing left to pay has ended; one without one never does.
    private suspend fun removeIfEnded(rule: PlannedPaymentRule) {
        if (rule.endDate != null) plannedPaymentRuleRepository.deleteById(rule.id)
    }

    /**
     * What each of the rule's payments moves: its amount, in its account's currency, and for a
     * transfer what arrives, in the other account's at today's rate. Null without its accounts.
     */
    private suspend fun values(rule: PlannedPaymentRule): Values? {
        val from = accountRepository.findById(rule.account)?.asset?.let { PositiveValue(rule.amount, it) }
        val toAccount = rule.toAccount
        return when {
            from == null -> null
            toAccount == null -> Values(from, to = null)
            else -> accountRepository.findById(toAccount)?.asset?.let { toAsset ->
                // Without a rate between them, the same amount: paying converts at the day's rate.
                val to = exchangeUseCase.convert(from, to = toAsset).getOrNull() ?: PositiveValue(rule.amount, toAsset)
                Values(from, to)
            }
        }
    }

    /** A planned payment's amount, and for a transfer what arrives. */
    private data class Values(val from: PositiveValue, val to: PositiveValue?)

    /**
     * The rule's occurrences that [include] accepts, up to [limit] of them, three years from now
     * (or from its start, if that is later), and the day of its end date. Each date counts from
     * the start date, so a monthly rule from 31 January falls on 28 February and then 31 March.
     */
    private fun occurrences(
        rule: PlannedPaymentRule,
        interval: PlannedPaymentInterval,
        values: Values,
        limit: Int,
        include: (Instant) -> Boolean,
    ): List<Transaction> {
        val end = maxOf(rule.startDate, timeProvider.utcNow()).plusSeconds(GenerationPeriodSeconds)
        val zone = timeProvider.getZoneId()
        val lastDay = rule.endDate?.atZone(zone)?.toLocalDate()
        val planned = mutableListOf<Transaction>()
        var step = 0L
        while (planned.size < limit) {
            val date = interval.type.incrementDate(rule.startDate, intervalN = step++ * interval.n.value, zone = zone)
            if (!date.isBefore(end) || lastDay != null && date.atZone(zone).toLocalDate().isAfter(lastDay)) break
            if (include(date)) planned += plannedTransaction(rule, values, dueDate = date)
        }
        return planned
    }

    /** The date a transaction was planned for: a paid one keeps it in its metadata. */
    private fun Transaction.dueDate(): Instant = metadata.paidForDateTime ?: time

    /** The unpaid transaction [rule] plans for [dueDate]. */
    private fun plannedTransaction(rule: PlannedPaymentRule, values: Values, dueDate: Instant): Transaction {
        val id = TransactionId(UUID.randomUUID())
        val value = values.from
        val toAccount = rule.toAccount
        val toValue = values.to
        val metadata = TransactionMetadata(
            recurringRuleId = rule.id.value,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
            purchaseDateTime = null,
            // Paid with the rule's card, it counts when the card charges it.
            creditCard = rule.creditCard,
        )
        return when {
            toAccount != null && toValue != null -> Transfer(
                id = id,
                title = rule.title,
                description = rule.description,
                category = rule.category,
                time = dueDate,
                settled = false,
                metadata = metadata,
                fromAccount = rule.account,
                fromValue = value,
                toAccount = toAccount,
                toValue = toValue,
            )

            rule.type == TransactionType.INCOME -> Income(
                id = id,
                title = rule.title,
                description = rule.description,
                category = rule.category,
                time = dueDate,
                settled = false,
                metadata = metadata,
                value = value,
                account = rule.account,
            )

            else -> Expense(
                id = id,
                title = rule.title,
                description = rule.description,
                category = rule.category,
                time = dueDate,
                settled = false,
                metadata = metadata,
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
