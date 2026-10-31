package com.ivy.wallet.domain.action.global

import com.ivy.data.preferences.AppPreferences
import com.ivy.wallet.PeriodState
import javax.inject.Inject

class StartDayOfMonthAct @Inject constructor(
    private val appPreferences: AppPreferences,
    private val periodState: PeriodState
) {
    suspend operator fun invoke(): Int {
        val startDay = appPreferences.startDayOfMonth()
        periodState.setStartDayOfMonth(startDay)
        return startDay
    }
}
