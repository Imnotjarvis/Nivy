package com.ivy.domain.usecase.planned

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.base.model.incrementDate
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
) {
    /**
     * Replaces the rule's unpaid transactions: one for a one-time rule, and for a recurring one,
     * up to [GeneratedInstancesLimit] in the three years from its start. Paid transactions are
     * kept and take the place of the first ones.
     */
    suspend fun generate(rule: PlannedPaymentRule) {
        transactionRepository.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId = rule.id.value)
        val paid = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id.value).size
        val asset = accountRepository.findById(rule.account)?.asset ?: return

        val interval = rule.interval
        if (interval == null) {
            if (paid == 0) save(rule, asset, dueDate = rule.startDate)
            return
        }

        val endDate = rule.startDate.plusSeconds(GenerationPeriodSeconds)
        var toSkip = paid
        var generated = 0
        var date = rule.startDate
        while (date.isBefore(endDate) && generated < GeneratedInstancesLimit) {
            if (toSkip > 0) {
                toSkip--
            } else {
                save(rule, asset, dueDate = date)
                generated++
            }
            date = interval.type.incrementDate(date, intervalN = interval.n.value.toLong())
        }
    }

    private suspend fun save(rule: PlannedPaymentRule, asset: AssetCode, dueDate: Instant) {
        val id = TransactionId(UUID.randomUUID())
        val value = PositiveValue(amount = rule.amount, asset = asset)
        val metadata = TransactionMetadata(
            recurringRuleId = rule.id.value,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
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
