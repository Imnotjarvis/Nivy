package com.ivy.domain.usecase.planned

import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PlannedPaymentRuleId
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.PlannedPaymentRuleRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.transaction.TransactionAmountsUseCase
import java.time.Instant
import javax.inject.Inject

/** Pays or skips the due transactions that planned payment rules generate. */
class DueTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionAmountsUseCase: TransactionAmountsUseCase,
    private val plannedPaymentsGenerator: PlannedPaymentsGenerator,
    private val timeProvider: TimeProvider,
) {
    /** Income minus expenses of what's due between [from] and [to], in the base currency. */
    suspend fun dueBalance(from: Instant, to: Instant): Double =
        transactionAmountsUseCase.balance(
            transactionRepository.findAllDueToBetween(startDate = from, endDate = to),
            currencyRepository.getBaseCurrency()
        )

    /**
     * Settles a due [transaction] now; its due time becomes the time it was paid for.
     * @return the paid transaction, or null if it was already settled
     */
    suspend fun pay(transaction: Transaction): Transaction? {
        if (transaction.settled) return null
        val paid = transaction.paidNow()
        transactionRepository.save(paid)
        afterHandled(transaction)
        return paid
    }

    /** Deletes the due [transactions] without paying them. */
    suspend fun skip(transactions: List<Transaction>) {
        transactions.filterNot { it.settled }.forEach {
            transactionRepository.deleteById(it.id)
            afterHandled(it)
        }
    }

    /**
     * Once a transaction is paid or skipped, a one-time rule has nothing left to plan, and a
     * recurring one gets more occurrences so that it keeps going.
     */
    private suspend fun afterHandled(transaction: Transaction) {
        val ruleId = transaction.metadata.recurringRuleId?.let(::PlannedPaymentRuleId) ?: return
        val rule = plannedPaymentRuleRepository.findById(ruleId) ?: return
        if (rule.interval == null) {
            plannedPaymentRuleRepository.deleteById(rule.id)
        } else {
            plannedPaymentsGenerator.topUp(rule)
        }
    }

    private fun Transaction.paidNow(): Transaction {
        val now = timeProvider.utcNow()
        val metadata = metadata.copy(paidForDateTime = time)
        return when (this) {
            is Income -> copy(settled = true, time = now, metadata = metadata)
            is Expense -> copy(settled = true, time = now, metadata = metadata)
            is Transfer -> copy(settled = true, time = now, metadata = metadata)
        }
    }
}
