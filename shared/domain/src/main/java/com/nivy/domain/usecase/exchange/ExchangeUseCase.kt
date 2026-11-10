package com.nivy.domain.usecase.exchange

import arrow.core.Option
import arrow.core.raise.option
import arrow.core.toOption
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.ExchangeRatesRepository
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Converts between currencies with the stored rates, which are relative to the base currency.
 * A conversion between two other currencies goes through the base currency.
 *
 * Without a rate it converts nothing, and every total leaves that amount out (counts it as 0),
 * as do the older totals, which convert with [exchange]: no total adds amounts in different currencies.
 */
class ExchangeUseCase @Inject constructor(
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val currencyRepository: CurrencyRepository,
    private val dispatchers: DispatchersProvider,
) {
    /**
     * Converts a [value] from one currency [to] another.
     *
     * @return some value if the conversion was successful or [arrow.core.None]
     */
    suspend fun convert(value: PositiveValue, to: AssetCode): Option<PositiveValue> = option {
        val amount = convertAmount(value.amount.value, from = value.asset, to = to).bind()
        PositiveValue(
            amount = PositiveDouble.from(amount).getOrNull().toOption().bind(),
            asset = to,
        )
    }

    /** Converts [amount] as [data] describes; empty when there's no rate for it. */
    suspend fun exchange(data: ExchangeData, amount: BigDecimal): Option<BigDecimal> = withContext(dispatchers.io) {
        com.nivy.wallet.domain.pure.exchange.exchange(
            data = data,
            amount = amount,
            getExchangeRate = { baseCurrency, currency ->
                // Rates are looked up by asset code; an invalid code has no rate.
                val base = AssetCode.from(baseCurrency).getOrNull()
                val target = AssetCode.from(currency).getOrNull()
                if (base != null && target != null) {
                    exchangeRatesRepository.findRate(baseCurrency = base, currency = target)?.rate?.value
                } else {
                    null
                }
            }
        )
    }

    private suspend fun convertAmount(
        amount: Double,
        from: AssetCode,
        to: AssetCode,
    ): Option<Double> = option {
        if (from == to) return@option amount

        when (val base = currencyRepository.getBaseCurrency()) {
            from -> amount * rate(base, to).bind()
            to -> amount / rate(base, from).bind()
            else -> amount / rate(base, from).bind() * rate(base, to).bind()
        }
    }

    private suspend fun rate(base: AssetCode, currency: AssetCode): Option<Double> =
        exchangeRatesRepository.findRate(baseCurrency = base, currency = currency)
            ?.rate?.value.toOption()
}
