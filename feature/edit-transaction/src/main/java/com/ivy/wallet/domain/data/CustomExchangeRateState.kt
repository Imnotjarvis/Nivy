package com.ivy.wallet.domain.data

import androidx.compose.runtime.Immutable

@Immutable
data class CustomExchangeRateState(
    val showCard: Boolean = false,
    val toCurrencyCode: String? = null,
    val fromCurrencyCode: String? = null,
    // Null while there is no rate for the pair: the transfer can't be saved until one is entered.
    val exchangeRate: Double? = null,
    val convertedAmount: Double? = null
)
