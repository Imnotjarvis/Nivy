package com.ivy.wallet.domain.action.global

import com.ivy.data.preferences.AppPreferences
import com.ivy.legacy.IvyWalletCtx
import javax.inject.Inject

class StartDayOfMonthAct @Inject constructor(
    private val appPreferences: AppPreferences,
    private val ivyWalletCtx: IvyWalletCtx
) {
    suspend operator fun invoke(): Int {
        val startDay = appPreferences.startDayOfMonth()
        ivyWalletCtx.setStartDayOfMonth(startDay)
        return startDay
    }
}
