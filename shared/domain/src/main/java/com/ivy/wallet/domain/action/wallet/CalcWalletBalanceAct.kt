package com.ivy.wallet.domain.action.wallet

import arrow.core.toOption
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalcWalletBalanceAct @Inject constructor(
    private val accountsAct: AccountsAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val exchangeAct: ExchangeAct,
) {
    suspend operator fun invoke(input: Input): BigDecimal = withContext(Dispatchers.IO) {
        accountsAct()
            .filter { input.withExcluded || it.includeInBalance }
            .map { account ->
                calcAccBalanceAct(CalcAccBalanceAct.Input(account = account, range = input.range))
            }
            .map {
                exchangeAct(
                    ExchangeAct.Input(
                        data = ExchangeData(
                            baseCurrency = input.baseCurrency,
                            fromCurrency = (it.account.asset.code).toOption(),
                            toCurrency = input.balanceCurrency
                        ),
                        amount = it.balance
                    )
                )
            }
            .fold(BigDecimal.ZERO) { sum, balance -> sum + (balance.getOrNull() ?: BigDecimal.ZERO) }
    }

    @Suppress("DataClassDefaultValues")
    data class Input(
        val baseCurrency: String,
        val balanceCurrency: String = baseCurrency,
        val range: ClosedTimeRange? = null,
        val withExcluded: Boolean = false
    )
}
