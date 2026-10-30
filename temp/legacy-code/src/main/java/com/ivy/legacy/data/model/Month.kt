package com.ivy.legacy.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.ui.R
import java.time.LocalDate

@Immutable
data class Month(
    val monthValue: Int,
    @StringRes val nameRes: Int,
) {
    companion object {
        fun monthsList(): MutableList<Month> = mutableListOf(
            Month(1, R.string.january),
            Month(2, R.string.february),
            Month(3, R.string.march),
            Month(4, R.string.april),
            Month(5, R.string.may),
            Month(6, R.string.june),
            Month(7, R.string.july),
            Month(8, R.string.august),
            Month(9, R.string.september),
            Month(10, R.string.october),
            Month(11, R.string.november),
            Month(12, R.string.december),
        )

        fun fromMonthValue(code: Int): Month =
            monthsList().first { it.monthValue == code }
    }

    fun toDate(): LocalDate =
        dateNowUTC()
            .withMonth(monthValue)

    // The month's name in the app's language.
    @Composable
    fun displayName(): String = stringResource(nameRes)

    fun incrementMonthPeriod(
        ivyContext: IvyWalletCtx,
        increment: Long,
        year: Int
    ): TimePeriod {
        val incrementedMonth = toDate().withYear(year).plusMonths(increment)
        val incrementedPeriod = TimePeriod(
            month = fromMonthValue(incrementedMonth.monthValue),
            year = incrementedMonth.year
        )
        ivyContext.updateSelectedPeriodInMemory(incrementedPeriod)
        return incrementedPeriod
    }
}
