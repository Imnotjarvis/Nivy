package com.nivy.domain.usecase.wallet

import arrow.core.toOption
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.repository.AccountRepository
import com.nivy.domain.usecase.account.AccountBalanceUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.pure.data.ClosedTimeRange
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class WalletBalanceUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountBalanceUseCase: AccountBalanceUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun balance(
        baseCurrency: String,
        range: ClosedTimeRange? = null,
        withExcluded: Boolean = false,
    ): BigDecimal = withContext(dispatchers.io) {
        accountRepository.findAll()
            .filter { withExcluded || it.includeInBalance }
            .map { account ->
                accountBalanceUseCase.balance(account = account, range = range)
            }
            .map {
                exchangeUseCase.exchange(
                    data = ExchangeData(
                        baseCurrency = baseCurrency,
                        fromCurrency = (it.account.asset.code).toOption(),
                        toCurrency = baseCurrency
                    ),
                    amount = it.balance
                )
            }
            .fold(BigDecimal.ZERO) { sum, balance -> sum + (balance.getOrNull() ?: BigDecimal.ZERO) }
    }
}
