package com.nivy.domain.usecase.transaction

import com.nivy.data.model.Transaction
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import javax.inject.Inject

/**
 * Sums transactions in one currency, each currency's rate read once. Without a rate, an amount is
 * left out (counts as 0), as in every other total.
 */
class TransactionAmountsUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
) {
    /** The sum of the transactions' amounts, a transfer counting what left its source account. */
    suspend fun total(transactions: List<Transaction>, currency: AssetCode): Double {
        val rates = BaseCurrencyRates(exchangeUseCase, currency)
        return transactions.sumOf { rates.inBase(it.getFromValue()) }
    }

    /** Income minus expenses; transfers don't change it. */
    suspend fun balance(transactions: List<Transaction>, currency: AssetCode): Double {
        val rates = BaseCurrencyRates(exchangeUseCase, currency)
        return transactions.sumOf { rates.signed(it) }
    }
}
