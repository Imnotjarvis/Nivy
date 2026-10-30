package com.ivy.wallet.domain.action.global

import com.ivy.data.preferences.AppPreferences
import com.ivy.frp.action.FPAction
import com.ivy.frp.then
import com.ivy.legacy.IvyWalletCtx
import javax.inject.Inject

class StartDayOfMonthAct @Inject constructor(
    private val appPreferences: AppPreferences,
    private val ivyWalletCtx: IvyWalletCtx
) : FPAction<Unit, Int>() {

    override suspend fun Unit.compose(): suspend () -> Int = suspend {
        appPreferences.startDayOfMonth()
    } then { startDay ->
        ivyWalletCtx.setStartDayOfMonth(startDay)
        startDay
    }
}
