package com.ivy.wallet.domain.action.viewmodel.home

import com.ivy.data.preferences.AppPreferences
import javax.inject.Inject

class ShouldHideBalanceAct @Inject constructor(
    private val appPreferences: AppPreferences
) {
    suspend operator fun invoke(): Boolean = appPreferences.hideCurrentBalance()
}
