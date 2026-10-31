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
        fun monthsList(): MutableList<Month> = listOf(
            R.string.january, R.string.february, R.string.march, R.string.april,
            R.string.may, R.string.june, R.string.july, R.string.august,
            R.string.september, R.string.october, R.string.november, R.string.december,
        ).mapIndexed { index, nameRes -> Month(index + 1, nameRes) }.toMutableList()

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
