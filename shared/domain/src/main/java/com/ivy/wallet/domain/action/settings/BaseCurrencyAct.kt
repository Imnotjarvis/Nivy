package com.ivy.wallet.domain.action.settings

import com.ivy.data.repository.CurrencyRepository
import javax.inject.Inject

class BaseCurrencyAct @Inject constructor(
    private val currencyRepository: CurrencyRepository,
) {
    suspend operator fun invoke(): String = currencyRepository.getBaseCurrency().code
}
