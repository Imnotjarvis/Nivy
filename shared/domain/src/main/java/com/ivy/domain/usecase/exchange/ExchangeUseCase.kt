package com.ivy.domain.usecase.exchange

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import arrow.core.toOption
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Value
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NonZeroDouble
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.ExchangeRatesRepository
import javax.inject.Inject

/**
 * Converts between currencies with the stored rates, which are relative to the base currency.
 * A conversion between two other currencies goes through the base currency.
 */
class ExchangeUseCase @Inject constructor(
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val currencyRepository: CurrencyRepository,
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

    /**
     * @return none exchanged value for empty [values] or if the
     * exchange fails for all [AssetCode]
     */
    suspend fun convert(
        values: Map<AssetCode, NonZeroDouble>,
        to: AssetCode
    ): ExchangeResult {
        val exchangeErrors = mutableSetOf<AssetCode>()
        val exchanged = values.mapNotNull { (asset, amount) ->
            convertAmount(amount.value, from = asset, to = to).getOrNull()
                .also { if (it == null) exchangeErrors += asset }
        }
        return ExchangeResult(
            exchanged = if (exchanged.isEmpty()) {
                None
            } else {
                NonZeroDouble.from(exchanged.sum()).getOrNull().toOption()
                    .map { Value(amount = it, asset = to) }
            },
            exchangeErrors = exchangeErrors,
        )
    }

    /**
     * Converts a [value] from one currency [to] another.
     *
     * @return some value if the conversion was successful or [arrow.core.None]
     */
    suspend fun convert(
        value: Value,
        to: AssetCode
    ): Option<Value> = option {
        val amount = convertAmount(value.amount.value, from = value.asset, to = to).bind()
        Value(
            amount = NonZeroDouble.from(amount).getOrNull().toOption().bind(),
            asset = to,
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

data class ExchangeResult(
    /**
     * Some value that was exchanged successfully.
     * Or [arrow.core.None] if there were errors or an empty map was exchanged.
     */
    val exchanged: Option<Value>,
    /**
     * Assets for which the exchange has failed.
     */
    val exchangeErrors: Set<AssetCode>,
)
