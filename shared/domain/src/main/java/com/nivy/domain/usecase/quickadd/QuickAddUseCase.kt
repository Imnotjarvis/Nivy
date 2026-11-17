package com.nivy.domain.usecase.quickadd

import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.QuickAdd
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import java.util.UUID
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
        val value = (quickAdd.amount ?: amount)?.let { amount ->
            accountRepository.findById(quickAdd.account)?.let { PositiveValue(amount, it.asset) }
        } ?: return null
        val toAccount = quickAdd.toAccount
        val toValue = if (toAccount != null) {
            val toAsset = accountRepository.findById(toAccount)?.asset ?: return null
            exchangeUseCase.convert(value, to = toAsset).getOrNull() ?: return null
        } else {
            null
        }
        val transaction = creditCardChargeUseCase.dated(transaction(quickAdd, value, toValue))
        transactionRepository.save(transaction)
        dataObserver.post(DataWriteEvent.TransactionsChanged)
        return transaction
    }

    /** Deletes the transaction a quick add saved. */
    suspend fun undo(transaction: TransactionId) {
        transactionRepository.deleteById(transaction)
        dataObserver.post(DataWriteEvent.TransactionsChanged)
    }

    private fun transaction(quickAdd: QuickAdd, value: PositiveValue, toValue: PositiveValue?): Transaction {
        val id = TransactionId(UUID.randomUUID())
        val time = timeProvider.utcNow()
        val metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
            creditCard = quickAdd.creditCard,
        )
        val toAccount = quickAdd.toAccount
        return when {
            toAccount != null && toValue != null -> Transfer(
                id = id,
                title = quickAdd.title,
                description = null,
                category = quickAdd.category,
                time = time,
                settled = true,
                metadata = metadata,
                fromAccount = quickAdd.account,
                fromValue = value,
                toAccount = toAccount,
                toValue = toValue,
            )

            quickAdd.type == TransactionType.INCOME -> Income(
                id = id,
                title = quickAdd.title,
                description = null,
                category = quickAdd.category,
                time = time,
                settled = true,
                metadata = metadata,
                value = value,
                account = quickAdd.account,
            )

            else -> Expense(
                id = id,
                title = quickAdd.title,
                description = null,
                category = quickAdd.category,
                time = time,
                settled = true,
                metadata = metadata,
                value = value,
                account = quickAdd.account,
            )
        }
    }
}
