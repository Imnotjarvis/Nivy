package com.nivy.domain.usecase.planned

import com.nivy.base.time.TimeProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PlannedPaymentRuleId
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.transaction.TransactionAmountsUseCase
import java.time.Instant
import javax.inject.Inject

/** Pays or skips the due transactions that planned payment rules generate. */
class DueTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionAmountsUseCase: TransactionAmountsUseCase,
    private val plannedPaymentsGenerator: PlannedPaymentsGenerator,
    private val creditCardChargeUseCase: CreditCardChargeUseCase,
    private val timeProvider: TimeProvider,
    private val databaseTransaction: DatabaseTransaction,
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
        // Paid with a credit card, it counts when the card charges it.
        val paid = creditCardChargeUseCase.dated(transaction.paidNow())
        // One database transaction: the payment and what it changes in its rule land together.
        databaseTransaction.run {
            transactionRepository.save(paid)
            afterHandled(transaction)
        }
        return paid
    }

    /** Deletes the due [transactions] without paying them. */
    suspend fun skip(transactions: List<Transaction>) {
        databaseTransaction.run {
            transactions.filterNot { it.settled }.forEach {
                transactionRepository.deleteById(it.id)
                afterHandled(it)
            }
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
