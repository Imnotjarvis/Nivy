package com.nivy.wallet.domain.pure.exchange

import arrow.core.Option
import arrow.core.toOption
import com.nivy.data.model.Transaction
import com.nivy.data.model.getFromValue
import com.nivy.data.temp.migration.getValue
import java.math.BigDecimal

typealias ExchangeEffect = suspend (ExchangeData, BigDecimal) -> Option<BigDecimal>

data class ExchangeTrnArgument(
    val baseCurrency: String,
    val exchange: ExchangeEffect
)

suspend fun exchangeInBaseCurrency(
    transaction: Transaction,
    arg: ExchangeTrnArgument
): BigDecimal = exchangeInBaseCurrency(
    transaction = transaction,
    baseCurrency = arg.baseCurrency,
    exchange = arg.exchange
)

/**
 * The transaction's amount in the base currency, from its own currency: a transfer's is what
 * left its account. Without a rate it counts as 0, as in every total.
 */
suspend fun exchangeInBaseCurrency(
    transaction: Transaction,
    baseCurrency: String,
    exchange: ExchangeEffect
): BigDecimal = exchange(
    ExchangeData(
        baseCurrency = baseCurrency,
        fromCurrency = transaction.getFromValue().asset.code.toOption(),
    ),
    transaction.getValue()
).getOrNull() ?: BigDecimal.ZERO
