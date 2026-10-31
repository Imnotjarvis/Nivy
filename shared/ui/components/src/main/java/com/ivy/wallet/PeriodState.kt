package com.ivy.wallet

import com.ivy.base.time.TimeProvider
import com.ivy.wallet.data.model.Month
import com.ivy.wallet.data.model.TimePeriod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The period the app's screens show, and the day of the month the user's months start on.
 * Screens share it, so picking a month on one shows that month on the others.
 */
@Singleton
class PeriodState @Inject constructor(
    private val timeProvider: TimeProvider,
) {
    var startDayOfMonth = 1
        private set

    var selected: TimePeriod = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
        private set

    private var selectedInitialized = false

    fun setStartDayOfMonth(day: Int) {
        startDayOfMonth = day
    }

    /** Selects the current month the first time, or again with [force]; returns the selection. */
    fun initSelected(startDayOfMonth: Int, force: Boolean = false): TimePeriod {
        if (!selectedInitialized || force) {
            selected = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
            selectedInitialized = true
        }
        return selected
    }

    fun select(period: TimePeriod) {
        selected = period
    }

    /** Selects the month [increment] months away from [month] of [year], and returns it. */
    fun selectMonth(month: Month, increment: Long, year: Int): TimePeriod =
        month.incrementMonthPeriod(increment, year).also { selected = it }
}
