package com.ivy.wallet.domain.action.global

import arrow.core.Either
import com.ivy.data.preferences.AppPreferences
import com.ivy.legacy.IvyWalletCtx
import javax.inject.Inject

class UpdateStartDayOfMonthAct @Inject constructor(
    private val appPreferences: AppPreferences,
    private val ivyWalletCtx: IvyWalletCtx
) {
    /** Saves a new first day for the app's months; the result is the saved day, or why it was refused. */
    suspend operator fun invoke(startDay: Int): Either<String, Int> {
        if (startDay !in 1..MaxStartDay) {
            return Either.Left("Invalid start day $startDay. Start date must be between 1 and 31.")
        }
        appPreferences.setStartDayOfMonth(startDay)
        ivyWalletCtx.setStartDayOfMonth(startDay)
        ivyWalletCtx.initSelectedPeriodInMemory(
            startDayOfMonth = startDay,
            forceReinitialize = true
        )
        return Either.Right(startDay)
    }

    private companion object {
        const val MaxStartDay = 31
    }
}
