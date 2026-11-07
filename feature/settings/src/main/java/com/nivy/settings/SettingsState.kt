package com.nivy.settings

import com.nivy.base.model.AppLockDelay
import com.nivy.base.model.DateDisplayFormat
import com.nivy.base.model.Theme

data class SettingsState(
    val currencyCode: String,
    val name: String,
    val currentTheme: Theme,
    val lockApp: Boolean,
    val lockDelay: AppLockDelay,
    val showNotifications: Boolean,
    val hideCurrentBalance: Boolean,
    val hideIncome: Boolean,
    val treatTransfersAsIncomeExpense: Boolean,
    val startDateOfMonth: String,
    val dateFormat: DateDisplayFormat,
    val progressState: Boolean,
)
