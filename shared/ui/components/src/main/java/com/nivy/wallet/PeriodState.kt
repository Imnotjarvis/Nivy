package com.nivy.wallet

import com.nivy.base.time.TimeProvider
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
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

    // Until the user picks a period, the selection is the current month, whichever it is now.
    private var pickedByUser = false

    fun setStartDayOfMonth(day: Int) {
        startDayOfMonth = day
    }

    /**
     * Selects the current month unless the user picked a period, or always with [force];
     * returns the selection.
     */
    fun initSelected(startDayOfMonth: Int, force: Boolean = false): TimePeriod {
        if (!pickedByUser || force) {
            selected = TimePeriod.currentMonth(startDayOfMonth, timeProvider)
            pickedByUser = false
        }
        return selected
    }

    fun select(period: TimePeriod) {
        selected = period
        pickedByUser = true
    }

    /** Selects the month [increment] months away from [month] of [year], and returns it. */
    fun selectMonth(month: Month, increment: Long, year: Int): TimePeriod =
        month.incrementMonthPeriod(increment, year).also { select(it) }
}
