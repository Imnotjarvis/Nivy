package com.nivy.wallet.domain.usecase

import arrow.core.Either
import com.nivy.data.preferences.AppPreferences
import com.nivy.wallet.PeriodState
import javax.inject.Inject

/** The day of the month the app's months start on, kept in the preferences and in [PeriodState]. */
class StartDayOfMonthUseCase @Inject constructor(
    private val appPreferences: AppPreferences,
    private val periodState: PeriodState
) {
    suspend fun startDay(): Int {
        val startDay = appPreferences.startDayOfMonth()
        periodState.setStartDayOfMonth(startDay)
        return startDay
    }

    /** Saves a new first day for the app's months; the result is the saved day, or why it was refused. */
    suspend fun setStartDay(startDay: Int): Either<String, Int> {
        if (startDay !in 1..MaxStartDay) {
            return Either.Left("Invalid start day $startDay. Start date must be between 1 and 31.")
        }
        appPreferences.setStartDayOfMonth(startDay)
        periodState.setStartDayOfMonth(startDay)
        periodState.initSelected(startDayOfMonth = startDay, force = true)
        return Either.Right(startDay)
    }

    private companion object {
        const val MaxStartDay = 31
    }
}
