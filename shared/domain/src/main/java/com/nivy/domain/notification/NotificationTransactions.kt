package com.nivy.domain.notification

import com.nivy.data.DataObserver
import com.nivy.data.DataWriteEvent
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.logic.SmartTitleSuggestionsLogic
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * A payment read from a notification, to add: [cents] of [currency] at [merchant], at [time], on
 * [account] or its [creditCard]; an income when [income].
 */
data class NotificationPayment(
    val cents: Long,
    val currency: String,
    val merchant: String?,
    val time: Instant,
    val account: AccountId,
    val creditCard: CreditCardId?,
    val income: Boolean = false,
)

/** Adds a notification's payment as a transaction, and takes it back. */
class NotificationTransactions @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val creditCardChargeUseCase: CreditCardChargeUseCase,
    private val titleSuggestions: SmartTitleSuggestionsLogic,
    private val exchangeUseCase: ExchangeUseCase,
    private val dataObserver: DataObserver,
) {
    /**
     * Saves [payment] as a paid expense, or an income, titled with its shop and in the category
     * that shop is in most, on its card's charge date when it's paid with one. In another currency
     * than the account's, it's converted at the day's rate. Null when it isn't saved: the account
     * is gone, or there's no rate.
     */
    suspend fun add(payment: NotificationPayment): TransactionId? {
        val value = valueOn(payment) ?: return null
        val transaction = creditCardChargeUseCase.dated(transaction(payment, value))
        transactionRepository.save(transaction)
        dataObserver.post(DataWriteEvent.TransactionsChanged)
        return transaction.id
    }

    /** Takes back what [add] saved. */
    suspend fun remove(id: TransactionId) {
        transactionRepository.deleteById(id)
        dataObserver.post(DataWriteEvent.TransactionsChanged)
    }

    // The payment's amount in its account's currency.
    private suspend fun valueOn(payment: NotificationPayment): PositiveValue? {
        val asset = accountRepository.findById(payment.account)?.asset
        val value = PositiveDouble.from(payment.cents.fromCents()).getOrNull()?.let { amount ->
            AssetCode.from(payment.currency).getOrNull()?.let { PositiveValue(amount, it) }
        }
        return when {
            asset == null || value == null -> null
            value.asset == asset -> value
            else -> exchangeUseCase.convert(value, to = asset).getOrNull()
        }
    }

    private suspend fun transaction(payment: NotificationPayment, value: PositiveValue): Transaction {
        val id = TransactionId(UUID.randomUUID())
        val title = payment.merchant?.let { NotBlankTrimmedString.from(it).getOrNull() }
        val category = payment.merchant?.let { titleSuggestions.categoryOf(it) }?.id
        val metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanRecordId = null,
            purchaseDateTime = null,
            creditCard = payment.creditCard.takeUnless { payment.income },
        )
        return if (payment.income) {
            Income(
                id = id,
                title = title,
                description = null,
                category = category,
                time = payment.time,
                settled = true,
                metadata = metadata,
                value = value,
                account = payment.account,
            )
        } else {
            Expense(
                id = id,
                title = title,
                description = null,
                category = category,
                time = payment.time,
                settled = true,
                metadata = metadata,
                value = value,
                account = payment.account,
            )
        }
    }
}
