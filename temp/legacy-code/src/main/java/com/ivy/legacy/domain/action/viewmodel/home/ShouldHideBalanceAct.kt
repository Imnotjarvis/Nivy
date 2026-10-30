package com.ivy.wallet.domain.action.viewmodel.home

import com.ivy.data.preferences.AppPreferences
import com.ivy.frp.action.FPAction
import javax.inject.Inject

class ShouldHideBalanceAct @Inject constructor(
    private val appPreferences: AppPreferences
) : FPAction<Unit, Boolean>() {
    override suspend fun Unit.compose(): suspend () -> Boolean = {
        appPreferences.hideCurrentBalance()
    }
}
