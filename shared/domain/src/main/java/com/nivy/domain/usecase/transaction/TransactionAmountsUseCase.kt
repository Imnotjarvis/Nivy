package com.nivy.domain.usecase.transaction

import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import javax.inject.Inject

/** Sums transactions in one currency. */
class TransactionAmountsUseCase @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
) {
    /** The sum of the transactions' amounts, a transfer counting what left its source account. */
    suspend fun total(transactions: List<Transaction>, currency: AssetCode): Double =
        transactions.sumOf { amountIn(it.getFromValue(), currency) }

    /** Income minus expenses; transfers don't change it. */
    suspend fun balance(transactions: List<Transaction>, currency: AssetCode): Double =
        transactions.sumOf {
            when (it) {
                is Income -> amountIn(it.value, currency)
                is Expense -> -amountIn(it.value, currency)
                is Transfer -> 0.0
            }
        }

    /** Without a rate, the amount is left out (counts as 0), as in every other total. */
    private suspend fun amountIn(value: PositiveValue, currency: AssetCode): Double =
        exchangeUseCase.convert(value, to = currency).getOrNull()?.amount?.value ?: 0.0
}
