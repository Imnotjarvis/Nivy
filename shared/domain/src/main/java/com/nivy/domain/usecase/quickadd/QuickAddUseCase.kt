package com.nivy.domain.usecase.quickadd

import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.domain.usecase.transaction.newTransaction
import javax.inject.Inject

/** Adds a quick add as a transaction made now, and takes it back. */
class QuickAddUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val creditCardChargeUseCase: CreditCardChargeUseCase,
    private val timeProvider: TimeProvider,
    private val dataObserver: DataObserver,
) {
    /**
     * Saves [quickAdd] as a paid transaction made now, of its own amount or else of [amount], on
     * its card's charge date when it's paid with one. A transfer between two currencies arrives
     * converted at the day's rate. Null when it isn't saved: without an amount, without its
     * accounts, or a transfer between currencies without a rate, which the editor asks for.
     */
    suspend fun add(quickAdd: QuickAdd, amount: PositiveDouble? = null): Transaction? {
        val values = values(quickAdd, amount) ?: return null
        val transaction = creditCardChargeUseCase.dated(transaction(quickAdd, values))
        transactionRepository.save(transaction)
        dataObserver.post(DataWriteEvent.TransactionsChanged)
        return transaction
    }

    /** Deletes the transaction a quick add saved. */
    suspend fun undo(transaction: TransactionId) {
        transactionRepository.deleteById(transaction)
        dataObserver.post(DataWriteEvent.TransactionsChanged)
    }

    /**
     * What [quickAdd] moves: its amount, else [amount], in its account's currency, and for a
     * transfer what arrives, in the other account's at the day's rate. Null without an amount,
     * without its accounts, or without a rate between them.
     */
    private suspend fun values(quickAdd: QuickAdd, amount: PositiveDouble?): Values? {
        val from = (quickAdd.amount ?: amount)?.let { value ->
            accountRepository.findById(quickAdd.account)?.let { PositiveValue(value, it.asset) }
        }
        val toAccount = quickAdd.toAccount
        return when {
            from == null -> null
            toAccount == null -> Values(from, to = null)
            else -> accountRepository.findById(toAccount)?.asset
                ?.let { exchangeUseCase.convert(from, to = it).getOrNull() }
                ?.let { Values(from, it) }
        }
    }

    /** A quick add's amount, and for a transfer what arrives. */
    private data class Values(val from: PositiveValue, val to: PositiveValue?)

    private fun transaction(quickAdd: QuickAdd, values: Values): Transaction = newTransaction(
        type = quickAdd.type,
        account = quickAdd.account,
        value = values.from,
        title = quickAdd.title,
        category = quickAdd.category,
        time = timeProvider.utcNow(),
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
            creditCard = quickAdd.creditCard,
        ),
        toAccount = quickAdd.toAccount,
        toValue = values.to,
    )
}
