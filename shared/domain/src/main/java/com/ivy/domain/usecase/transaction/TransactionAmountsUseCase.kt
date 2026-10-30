package com.ivy.domain.usecase.transaction

import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.getFromValue
import com.ivy.data.model.primitive.AssetCode
import com.ivy.domain.usecase.exchange.ExchangeUseCase
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

    /** Without a rate, the amount counts unconverted, as it always has. */
    private suspend fun amountIn(value: PositiveValue, currency: AssetCode): Double =
        exchangeUseCase.convert(value, to = currency).getOrNull()?.amount?.value
            ?: value.amount.value
}
