package com.nivy.domain.usecase.wallet

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.repository.AccountRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class WalletBalanceUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val dispatchers: DispatchersProvider,
) {
    /** The accounts' balances, those in the balance or all of them, in the [baseCurrency]. */
    suspend fun balance(
        baseCurrency: String,
        range: ClosedTimeRange? = null,
        withExcluded: Boolean = false,
    ): BigDecimal = withContext(dispatchers.io) {
        val rates = BaseCurrencyRates(exchangeUseCase, AssetCode.unsafe(baseCurrency))
        accountRepository.findAll()
            .filter { withExcluded || it.includeInBalance }
            .sumOf { account ->
                val balance = accountBalanceUseCase.balance(account = account, range = range).balance
                rates.inBase(balance.toDouble(), account.asset)
            }
            .toBigDecimal()
    }
}
