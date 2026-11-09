package com.nivy.balance

import androidx.compose.runtime.Immutable
import com.nivy.balance.detail.MonthDetail
import com.nivy.wallet.data.model.TimePeriod

@Immutable
data class BalanceState(
    val period: TimePeriod,
    val baseCurrencyCode: String,
    val currentBalance: Double,
    val plannedPaymentsAmount: Double,
    val balanceAfterPlannedPayments: Double,
    // The month in detail, once loaded.
    val detail: MonthDetail? = null,
)
