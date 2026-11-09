package com.nivy.domain.usecase.exchange

import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble

/**
 * Amounts in the [base] currency, each currency's rate read once: for a screen that converts many
 * amounts at a time. Without a rate, an amount is left out, as in every other total.
 */
class BaseCurrencyRates(private val exchangeUseCase: ExchangeUseCase, private val base: AssetCode) {
    private val factors = mutableMapOf<AssetCode, Double?>()

    /** What one of [currency] is worth in the base currency, or null without a rate. */
    suspend fun factor(currency: AssetCode): Double? = factors.getOrPut(currency) {
        if (currency == base) {
            1.0
        } else {
            exchangeUseCase.convert(PositiveValue(PositiveDouble.unsafe(1.0), currency), to = base)
                .getOrNull()?.amount?.value
        }
    }

    suspend fun inBase(value: PositiveValue): Double = inBase(value.amount.value, value.asset)

    suspend fun inBase(amount: Double, currency: AssetCode): Double = amount * (factor(currency) ?: 0.0)

    /** A transaction's amount, income up and expenses down; a transfer stays within the wallet. */
    suspend fun signed(transaction: Transaction): Double = when (transaction) {
        is Income -> inBase(transaction.value)
        is Expense -> -inBase(transaction.value)
        is Transfer -> 0.0
    }
}
