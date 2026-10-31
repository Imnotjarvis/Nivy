package com.ivy.ui.time.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

// Material 3 date and time picker dialogs, drawn by Content(). Dismissing a dialog picks nothing.
@Stable
interface DateTimePicker {
    @Composable
    fun Content()

    /** Shows the date picker on the device-local date of [initialDate]. */
    fun pickDate(
        initialDate: Instant?,
        onDatePick: (LocalDate) -> Unit
    )

    /** Shows the date picker on [initialDate]; only dates from [minDate] to [maxDate] can be picked. */
    fun pickDate(
        initialDate: LocalDate?,
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        onDatePick: (LocalDate) -> Unit
    )

    fun pickTime(
        initialTime: LocalTime?,
        onTimePick: (LocalTime) -> Unit
    )
}
