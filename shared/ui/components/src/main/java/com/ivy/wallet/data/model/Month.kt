package com.ivy.wallet.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
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

    /** The first day of this month in [year]. */
    fun firstDay(year: Int): LocalDate = LocalDate.of(year, monthValue, 1)

    /** The month [increment] months away from this month of [year]. */
    fun incrementMonthPeriod(increment: Long, year: Int): TimePeriod {
        val incrementedMonth = firstDay(year).plusMonths(increment)
        return TimePeriod(
            month = fromMonthValue(incrementedMonth.monthValue),
            year = incrementedMonth.year
        )
    }

    // The month's name in the app's language.
    @Composable
    @ReadOnlyComposable
    fun displayName(): String = stringResource(nameRes)
}
