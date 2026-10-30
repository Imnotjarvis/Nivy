package com.ivy.wallet.domain.action.exchange

import arrow.core.Option
import com.ivy.data.db.dao.read.ExchangeRatesDao
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.domain.pure.exchange.exchange
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExchangeAct @Inject constructor(
    private val exchangeRatesDao: ExchangeRatesDao,
) {
    suspend operator fun invoke(input: Input): Option<BigDecimal> = withContext(Dispatchers.IO) {
        exchange(
            data = input.data,
            amount = input.amount,
            getExchangeRate = { baseCurrency, currency ->
                exchangeRatesDao.findByBaseCurrencyAndCurrency(baseCurrency, currency)?.rate
            }
        )
    }

    /** Converts [amount] as [data] describes; empty when there's no rate for it. */
    suspend fun convert(data: ExchangeData, amount: BigDecimal): Option<BigDecimal> =
        this(Input(data = data, amount = amount))

    data class Input(
        val data: ExchangeData,
        val amount: BigDecimal
    )
}
